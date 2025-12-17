import React, { useState, useEffect, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import ApiService from "../../services/ApiService";
import { useError } from "../common/ErrorDisplay";

const MenuDetailsPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [menu, setMenu] = useState(null);
  const [averageRating, setAverageRating] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const [cartSuccess, setCartSuccess] = useState(false);

  const isAuthenticated = ApiService.isAthenticated();
  const { ErrorDisplay, showError } = useError();

  // ✅ FIX: memoized fetch function
  const fetchMenu = useCallback(async () => {
    try {
      const response = await ApiService.getMenuById(id);
      if (response.statusCode === 200) {
        setMenu(response.data);

        const ratingResponse = await ApiService.getMenuAverageOverallReview(id);

        if (ratingResponse.statusCode === 200) {
          setAverageRating(ratingResponse.data);
        }
      } else {
        showError(response.message);
      }
    } catch (error) {
      showError(error.response?.data?.message || error.message);
    }
  }, [id, showError]);

  // ✅ FIX: correct dependency array
  useEffect(() => {
    fetchMenu();
  }, [fetchMenu]);

  const handleBackToMenu = () => {
    navigate(-1);
  };

  const handleAddToCart = async () => {
    if (!isAuthenticated) {
      showError(
        "Please login to continue, If you don't have an account do well to register"
      );
      setTimeout(() => navigate("/login"), 5000);
      return;
    }

    setCartSuccess(false);
    try {
      const response = await ApiService.addItemToCart({
        menuId: menu.id,
        quantity,
      });

      if (response.statusCode === 200) {
        setCartSuccess(true);
        setTimeout(() => setCartSuccess(false), 4000);
      } else {
        showError(response.message);
      }
    } catch (error) {
      showError(error.response?.data?.message || error.message);
    }
  };

  const incrementQuantity = () => setQuantity((prev) => prev + 1);
  const decrementQuantity = () =>
    quantity > 1 && setQuantity((prev) => prev - 1);

  if (!menu) return null;

  return (
    <div className="menu-details-container">
      <ErrorDisplay />

      <button onClick={handleBackToMenu} className="back-button">
        &larr; Back to Menu
      </button>

      {/* UI giữ nguyên */}
    </div>
  );
};

export default MenuDetailsPage;
