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
        const orders = ordersResponse.data.content;
        const menu = menuResponse.data;
        const payments = paymentsResponse.data;
        const activeCustomers = activeCustomerResponse.data;

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
      }
    } catch (error) {
      showError(error.response?.data?.message || error.message);
    }
  }, [showError]);

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  const handleViewOrder = (id) => {
    navigate(`/admin/orders/${id}`);
  };

  /* ===== CHART DATA (GIỮ NGUYÊN) ===== */
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
        <button className="refresh-btn" onClick={fetchDashboardData}>
          Refresh Data
        </button>
      </div>

      {/* UI giữ nguyên */}
    </div>
  );
};

export default AdminDashboardPage;
