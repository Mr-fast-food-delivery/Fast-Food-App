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
  const [loading, setLoading] = useState(true);

  const isAuthenticated = ApiService.isAthenticated();
  const { ErrorDisplay, showError } = useError();

  // ===== FETCH MENU DETAILS =====
  const fetchMenu = useCallback(async () => {
    try {
      setLoading(true);

      const response = await ApiService.getMenuById(id);
      if (response.statusCode === 200) {
        setMenu(response.data);

        // Fetch average rating
        const ratingResponse = await ApiService.getMenuAverageOverallReview(id);

        if (ratingResponse.statusCode === 200) {
          const ratingValue = Number(ratingResponse.data ?? 0);
          setAverageRating(Number.isFinite(ratingValue) ? ratingValue : 0);
        }
      } else {
        showError(response.message || "Menu not found");
      }
    } catch (error) {
      showError(error.response?.data?.message || error.message);
    } finally {
      setLoading(false);
    }
  }, [id, showError]);

  useEffect(() => {
    fetchMenu();
  }, [fetchMenu]);

  // ===== HANDLERS =====
  const handleBackToMenu = () => {
    navigate(-1);
  };

  const handleAddToCart = async () => {
    if (!isAuthenticated) {
      showError("Please login to continue");
      setTimeout(() => navigate("/login"), 3000);
      return;
    }

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

  // ===== LOADING UI =====
  if (loading) {
    return (
      <div className="menu-details-container" style={{ padding: 40 }}>
        <ErrorDisplay />
        <p>Loading menu details...</p>
      </div>
    );
  }

  // ===== NOT FOUND =====
  if (!menu) {
    return (
      <div className="menu-details-container" style={{ padding: 40 }}>
        <ErrorDisplay />
        <p>Menu item not found.</p>
        <button onClick={handleBackToMenu} className="back-button">
          &larr; Back to Menu
        </button>
      </div>
    );
  }

  // ===== MAIN UI =====
  return (
    <div className="menu-details-container">
      <ErrorDisplay />

      <button onClick={handleBackToMenu} className="back-button">
        &larr; Back to Menu
      </button>

      <div className="menu-item-header">
        <div className="menu-item-image-container">
          <img
            src={menu.imageUrl}
            alt={menu.name}
            className="menu-item-image-detail"
          />
        </div>

        <div className="menu-item-info">
          <h1 className="menu-item-name">{menu.name}</h1>
          <p className="menu-item-description">{menu.description}</p>

          <div className="menu-item-price-rating">
            <span className="price">${menu.price.toFixed(2)}</span>
            <div className="rating">
              <span className="rating-value">{averageRating.toFixed(1)}</span>
              <span className="rating-star">★</span>
            </div>
          </div>

          <div className="add-to-cart-section">
            <div className="quantity-selector">
              <button onClick={decrementQuantity} disabled={quantity <= 1}>
                -
              </button>
              <span>{quantity}</span>
              <button onClick={incrementQuantity}>+</button>
            </div>

            <button onClick={handleAddToCart} className="add-to-cart-btn">
              Add to Cart
            </button>

            {cartSuccess && (
              <div className="cart-success-message">
                Added to cart successfully!
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default MenuDetailsPage;
