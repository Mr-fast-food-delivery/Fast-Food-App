import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import ApiService from "../../services/ApiService";
import { useError } from "../common/ErrorDisplay";
import { Pie, Line } from "react-chartjs-2";
import { Chart, registerables } from "chart.js";

Chart.register(...registerables);

const AdminDashboardPage = () => {
  const { ErrorDisplay, showError } = useError();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);

  const [stats, setStats] = useState({
    totalOrders: 0,
    totalRevenue: 0,
    activeCustomers: 0,
    menu: 0,
    recentOrders: [],
    orderStatusDistribution: {},
    revenueData: [],
    popularItems: [],
  });

  const fetchDashboardData = useCallback(async () => {
    try {
      setLoading(true);

      const ordersResponse = await ApiService.getAllOrders();
      const menuResponse = await ApiService.getAllMenus();
      const paymentsResponse = await ApiService.getAllPayments();
      const activeCustomerResponse =
        await ApiService.countTotalActiveCustomers();

      if (
        ordersResponse.statusCode === 200 &&
        menuResponse.statusCode === 200 &&
        paymentsResponse.statusCode === 200
      ) {
        const orders = ordersResponse.data?.content || [];
        const menu = menuResponse.data || [];
        const payments = paymentsResponse.data || [];
        const activeCustomers = activeCustomerResponse.data || 0;

        const totalOrders = orders.length;
        const recentOrders = orders.slice(0, 5);

        const statusCounts = orders.reduce((acc, order) => {
          acc[order.orderStatus] = (acc[order.orderStatus] || 0) + 1;
          return acc;
        }, {});

        const itemCounts = {};
        orders.forEach((order) => {
          order.orderItems.forEach((item) => {
            itemCounts[item.menu.name] =
              (itemCounts[item.menu.name] || 0) + item.quantity;
          });
        });

        const popularItems = Object.entries(itemCounts)
          .sort((a, b) => b[1] - a[1])
          .slice(0, 5);

        const totalRevenue = payments.reduce(
          (sum, p) => (p.paymentStatus === "COMPLETED" ? sum + p.amount : sum),
          0
        );

        const revenueByMonth = Array(12).fill(0);
        payments.forEach((payment) => {
          if (payment.paymentStatus === "COMPLETED") {
            const month = new Date(payment.paymentDate).getMonth();
            revenueByMonth[month] += payment.amount;
          }
        });

        setStats({
          totalOrders,
          totalRevenue,
          activeCustomers,
          menu: menu.length,
          recentOrders,
          orderStatusDistribution: statusCounts,
          revenueData: revenueByMonth,
          popularItems,
        });
      } else {
        showError("Failed to load dashboard data");
      }
    } catch (error) {
      showError(error.response?.data?.message || error.message);
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  const handleViewOrder = (id) => {
    navigate(`/admin/orders/${id}`);
  };

  const revenueChartData = {
    labels: [
      "Jan",
      "Feb",
      "Mar",
      "Apr",
      "May",
      "Jun",
      "Jul",
      "Aug",
      "Sep",
      "Oct",
      "Nov",
      "Dec",
    ],
    datasets: [
      {
        label: "Monthly Revenue ($)",
        data: stats.revenueData,
        backgroundColor: "rgba(54, 162, 235, 0.2)",
        borderColor: "rgba(54, 162, 235, 1)",
        borderWidth: 1,
      },
    ],
  };

  const statusChartData = {
    labels: Object.keys(stats.orderStatusDistribution),
    datasets: [
      {
        data: Object.values(stats.orderStatusDistribution),
        backgroundColor: [
          "rgba(255, 99, 132, 0.7)",
          "rgba(54, 162, 235, 0.7)",
          "rgba(255, 206, 86, 0.7)",
          "rgba(75, 192, 192, 0.7)",
          "rgba(153, 102, 255, 0.7)",
          "rgba(255, 159, 64, 0.7)",
        ],
      },
    ],
  };

  return (
    <div className="admin-dashboard">
      <ErrorDisplay />
      <div className="content-header">
        <h1>Dashboard Overview</h1>
        <button
          className="refresh-btn"
          onClick={fetchDashboardData}
          disabled={loading}
        >
          {loading ? "Refreshing..." : "Refresh Data"}
        </button>
      </div>

      {loading ? (
        <p>Loading dashboard...</p>
      ) : (
        <>
          <div className="dashboard-cards">
            <div className="card">
              <p className="card-label">Total Orders</p>
              <h2>{stats.totalOrders}</h2>
            </div>
            <div className="card">
              <p className="card-label">Total Revenue</p>
              <h2>${stats.totalRevenue.toFixed(2)}</h2>
            </div>
            <div className="card">
              <p className="card-label">Active Customers</p>
              <h2>{stats.activeCustomers}</h2>
            </div>
            <div className="card">
              <p className="card-label">Menu Items</p>
              <h2>{stats.menu}</h2>
            </div>
          </div>

          <div className="dashboard-charts">
            <div className="chart-card">
              <h3>Monthly Revenue</h3>
              <Line data={revenueChartData} />
            </div>
            <div className="chart-card">
              <h3>Order Status</h3>
              <Pie data={statusChartData} />
            </div>
          </div>

          <div className="dashboard-tables">
            <div className="table-card">
              <h3>Recent Orders</h3>
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Customer</th>
                    <th>Status</th>
                    <th>Total</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {stats.recentOrders.length === 0 ? (
                    <tr>
                      <td colSpan="5">No recent orders</td>
                    </tr>
                  ) : (
                    stats.recentOrders.map((order) => (
                      <tr key={order.id}>
                        <td>{order.id}</td>
                        <td>{order.user?.name || "N/A"}</td>
                        <td>{order.orderStatus}</td>
                        <td>${order.totalAmount?.toFixed(2) || "0.00"}</td>
                        <td>
                          <button onClick={() => handleViewOrder(order.id)}>
                            View
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            <div className="table-card">
              <h3>Popular Items</h3>
              <table>
                <thead>
                  <tr>
                    <th>Item</th>
                    <th>Quantity Sold</th>
                  </tr>
                </thead>
                <tbody>
                  {stats.popularItems.length === 0 ? (
                    <tr>
                      <td colSpan="2">No popular items yet</td>
                    </tr>
                  ) : (
                    stats.popularItems.map(([name, qty]) => (
                      <tr key={name}>
                        <td>{name}</td>
                        <td>{qty}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default AdminDashboardPage;
