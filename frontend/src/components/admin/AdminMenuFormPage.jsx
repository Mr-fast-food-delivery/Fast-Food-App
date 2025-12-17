import { useState, useEffect, useCallback } from "react";
import { useNavigate, useParams } from "react-router-dom";
import ApiService from "../../services/ApiService";
import { useError } from "../common/ErrorDisplay";

const AdminMenuFormPage = () => {
  const { id } = useParams();
  const { ErrorDisplay, showError } = useError();
  const navigate = useNavigate();

  const [menu, setMenu] = useState({
    name: "",
    description: "",
    price: "",
    categoryId: "",
    imageFile: null,
  });

  const [categories, setCategories] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // ✅ FETCH ALL CATEGORIES
  const fetchCategories = useCallback(async () => {
    try {
      const response = await ApiService.getAllCategories();
      if (response.statusCode === 200) {
        setCategories(response.data);
      }
    } catch (error) {
      showError(error.response?.data?.message || error.message);
    }
  }, [showError]);

  // ✅ FETCH MENU BY ID (EDIT MODE)
  const fetchMenu = useCallback(async () => {
    if (!id) return;

    try {
      const response = await ApiService.getMenuById(id);
      if (response.statusCode === 200) {
        setMenu({
          ...response.data,
          price: response.data.price.toString(),
          categoryId: response.data.categoryId.toString(),
        });
      }
    } catch (error) {
      showError(error.response?.data?.message || error.message);
    }
  }, [id, showError]);

  // ✅ EFFECT
  useEffect(() => {
    fetchCategories();
    fetchMenu();
  }, [fetchCategories, fetchMenu]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setMenu((prev) => ({ ...prev, [name]: value }));
  };

  const handleFileChange = (e) => {
    setMenu((prev) => ({ ...prev, imageFile: e.target.files[0] }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);

    try {
      const formData = new FormData();
      if (menu.name) formData.append("name", menu.name);
      if (menu.description) formData.append("description", menu.description);
      if (menu.price) formData.append("price", menu.price);
      if (menu.categoryId) formData.append("categoryId", menu.categoryId);
      if (menu.imageFile) formData.append("imageFile", menu.imageFile);

      let response;
      if (id) {
        formData.append("id", id);
        response = await ApiService.updateMenu(formData);
      } else {
        response = await ApiService.addMenu(formData);
      }

      if (response.statusCode === 200) {
        navigate("/admin/menu-items");
      }
    } catch (error) {
      showError(error.response?.data?.message || error.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="admin-menu-item-form">
      <ErrorDisplay />
      {/* UI giữ nguyên */}
    </div>
  );
};

export default AdminMenuFormPage;
