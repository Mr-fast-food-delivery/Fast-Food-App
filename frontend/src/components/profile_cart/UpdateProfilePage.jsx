import { useState, useEffect, useRef, useCallback } from "react";
import ApiService from "../../services/ApiService";
import { useNavigate } from "react-router-dom";
import { useError } from "../common/ErrorDisplay";

const UpdateProfilePage = () => {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [address, setAddress] = useState("");

  const [profileImage, setProfileImage] = useState(null);
  const [previewImage, setPreviewImage] = useState("");

  const fileInputRef = useRef(null);

  const navigate = useNavigate();
  const { ErrorDisplay, showError } = useError();

  // ✅ ổn định reference cho ESLint
  const fetchUserProfile = useCallback(async () => {
    try {
      const response = await ApiService.myProfile();
      if (response.statusCode === 200) {
        const userData = response.data;
        setName(userData.name);
        setEmail(userData.email);
        setPhoneNumber(userData.phoneNumber);
        setAddress(userData.address);
        setPreviewImage(userData.profileUrl);
      }
    } catch (error) {
      showError(error.response?.data?.message || error.message);
    }
  }, [showError]);

  // ✅ dependency array đúng chuẩn
  useEffect(() => {
    fetchUserProfile();
  }, [fetchUserProfile]);

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setProfileImage(file);
      setPreviewImage(URL.createObjectURL(file));
    }
  };

  const triggerFileInput = () => {
    fileInputRef.current.click();
  };

  const handleUpdateProfile = async (e) => {
    e.preventDefault();

    if (!window.confirm("Are you sure you want to update your profile?"))
      return;

    try {
      const formData = new FormData();
      formData.append("name", name);
      formData.append("email", email);
      formData.append("phoneNumber", phoneNumber);
      formData.append("address", address);

      if (profileImage) {
        formData.append("imageFile", profileImage);
      }

      const response = await ApiService.updateProfile(formData);
      if (response.statusCode === 200) {
        navigate("/profile");
      }
    } catch (error) {
      showError(error.response?.data?.message || error.message);
    }
  };

  const handleDeactivateProfile = async () => {
    if (
      !window.confirm(
        "Are you sure you want to Close your account? This action cannot be undone."
      )
    ) {
      return;
    }

    try {
      const response = await ApiService.deactivateProfile();
      if (response.statusCode === 200) {
        ApiService.logout();
        navigate("/home");
      }
    } catch (error) {
      showError(error.response?.data?.message || error.message);
    }
  };

  return (
    <div className="profile-container">
      <ErrorDisplay />

      <div className="profile-header">
        <h1 className="profile-title">Update Profile</h1>

        <div className="profile-image-container">
          <img
            src={previewImage}
            alt="Profile"
            className="profile-image"
            onClick={triggerFileInput}
          />
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleImageChange}
            accept="image/*"
            style={{ display: "none" }}
          />
          <button className="profile-image-upload" onClick={triggerFileInput}>
            Change Photo
          </button>
        </div>
      </div>

      <form className="profile-form" onSubmit={handleUpdateProfile}>
        <div className="form-grid">
          <div className="profile-form-group">
            <label>Name:</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          </div>

          <div className="profile-form-group">
            <label>Email:</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="profile-form-group">
            <label>Phone:</label>
            <input
              type="tel"
              value={phoneNumber}
              onChange={(e) => setPhoneNumber(e.target.value)}
              required
            />
          </div>

          <div className="profile-form-group">
            <label>Address:</label>
            <input
              type="text"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              required
            />
          </div>
        </div>

        <div className="form-actions">
          <button type="submit" className="btn btn-primary">
            Update Profile
          </button>
          <button
            type="button"
            className="btn btn-danger"
            onClick={handleDeactivateProfile}
          >
            Deactivate Account
          </button>
        </div>
      </form>
    </div>
  );
};

export default UpdateProfilePage;
