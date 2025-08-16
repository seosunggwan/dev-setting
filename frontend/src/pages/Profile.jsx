import React, { useState, useEffect } from "react";
import {
  Container,
  Typography,
  Box,
  TextField,
  Button,
  Grid,
  Paper,
  Divider,
  Snackbar,
  Alert,
  CircularProgress,
} from "@mui/material";
import { useLogin } from "../contexts/AuthContext";
import { fetchUserProfile, updateUserProfile } from "../services/userService";
import { updateProfileWithImage } from "../services/profileService";
import ProfileImageUploader from "../components/ProfileImageUploader";

const Profile = () => {
  const { isLoggedIn } = useLogin();
  const [loading, setLoading] = useState(true);
  const [profile, setProfile] = useState({
    username: "",
    email: "",
    city: "",
    street: "",
    zipcode: "",
    profileImageUrl: "",
  });
  const [editMode, setEditMode] = useState(false);
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: "",
    severity: "success",
  });

  // 프로필 정보 가져오기
  const fetchProfile = async () => {
    try {
      setLoading(true);
      const profileData = await fetchUserProfile();
      setProfile(profileData);
    } catch (error) {
      console.error("프로필 정보 가져오기 실패:", error);
      setSnackbar({
        open: true,
        message: error.message || "프로필 정보를 가져오는데 실패했습니다.",
        severity: "error",
      });
    } finally {
      setLoading(false);
    }
  };

  // 컴포넌트 마운트 시 프로필 정보 가져오기
  useEffect(() => {
    if (isLoggedIn) {
      fetchProfile();
    }
  }, [isLoggedIn]);

  // 입력 필드 변경 핸들러
  const handleChange = (e) => {
    const { name, value } = e.target;
    console.log(`필드 변경: ${name} = ${value}`); // 디버깅 로그 추가
    setProfile((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  // 프로필 업데이트 핸들러
  const handleUpdateProfile = async () => {
    try {
      console.log("프로필 업데이트 시작:", profile); // 디버깅 로그 추가
      setLoading(true);
      const updatedProfile = await updateUserProfile(profile);
      console.log("프로필 업데이트 성공:", updatedProfile); // 디버깅 로그 추가
      setProfile(updatedProfile);
      setEditMode(false);
      setSnackbar({
        open: true,
        message: "프로필이 성공적으로 업데이트되었습니다.",
        severity: "success",
      });
    } catch (error) {
      console.error("프로필 업데이트 실패:", error);
      let errorMessage = "프로필 업데이트에 실패했습니다.";
      
      // 더 자세한 에러 메시지 처리
      if (error.response?.data?.message) {
        errorMessage = error.response.data.message;
      } else if (error.message) {
        errorMessage = error.message;
      }
      
      setSnackbar({
        open: true,
        message: errorMessage,
        severity: "error",
      });
    } finally {
      setLoading(false);
    }
  };

  // 프로필 이미지 업로드 완료 핸들러
  const handleProfileImageUploaded = async (imageUrl) => {
    try {
      // 이미지 URL로 프로필 정보 업데이트
      await updateProfileWithImage(imageUrl);

      // 상태 업데이트
      setProfile((prev) => ({
        ...prev,
        profileImageUrl: imageUrl,
      }));

      // 성공 메시지 표시 (ProfileImageUploader에서 이미 표시하므로 여기서는 생략)
    } catch (error) {
      console.error("프로필 이미지 업데이트 실패:", error);
      setSnackbar({
        open: true,
        message: error.message || "프로필 이미지 업데이트에 실패했습니다.",
        severity: "error",
      });
    }
  };

  // 스낵바 닫기 핸들러
  const handleCloseSnackbar = () => {
    setSnackbar((prev) => ({
      ...prev,
      open: false,
    }));
  };

  if (!isLoggedIn) {
    return (
      <Container maxWidth="md" sx={{ mt: 4, mb: 4 }}>
        <Box sx={{ textAlign: "center", py: 5 }}>
          <Typography variant="h5" gutterBottom>
            로그인이 필요합니다.
          </Typography>
        </Box>
      </Container>
    );
  }

  return (
    <Container maxWidth="md" sx={{ mt: 4, mb: 4 }}>
      <Paper elevation={3} sx={{ p: 4 }}>
        <Box
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            mb: 3,
          }}
        >
          <Typography variant="h4" component="h1" gutterBottom>
            내 프로필
          </Typography>
          {!editMode ? (
            <Button
              variant="contained"
              color="primary"
              size="large"
              onClick={() => {
                console.log("수정하기 버튼 클릭됨, editMode를 true로 설정"); // 디버깅 로그
                setEditMode(true);
              }}
              disabled={loading}
              sx={{
                fontSize: "1.1rem",
                fontWeight: "bold",
                px: 3,
                py: 1.5,
                boxShadow: 2,
                "&:hover": {
                  boxShadow: 4,
                }
              }}
            >
              ✏️ 수정하기
            </Button>
          ) : (
            <Box>
              <Button
                variant="contained"
                color="success"
                size="large"
                onClick={handleUpdateProfile}
                disabled={loading}
                sx={{ mr: 1, px: 3, py: 1.5 }}
              >
                💾 저장
              </Button>
              <Button
                variant="outlined"
                size="large"
                onClick={() => {
                  console.log("취소 버튼 클릭됨, editMode를 false로 설정"); // 디버깅 로그
                  setEditMode(false);
                  fetchProfile(); // 원래 정보로 되돌리기
                }}
                disabled={loading}
                sx={{ px: 3, py: 1.5 }}
              >
                ❌ 취소
              </Button>
            </Box>
          )}
        </Box>

        {/* 현재 상태 표시 (디버깅용) */}
        <Box sx={{ mb: 2, p: 1, bgcolor: "grey.100", borderRadius: 1 }}>
          <Typography variant="body2" color="text.secondary">
            현재 상태: {editMode ? "수정 모드" : "보기 모드"} | 
            로딩: {loading ? "예" : "아니오"}
          </Typography>
        </Box>

        <Divider sx={{ mb: 3 }} />

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", p: 4 }}>
            <CircularProgress />
          </Box>
        ) : (
          <Grid container spacing={3}>
            {/* 프로필 이미지 업로더 */}
            <Grid
              item
              xs={12}
              sx={{ display: "flex", justifyContent: "center", mb: 2 }}
            >
              <ProfileImageUploader
                initialImage={profile.profileImageUrl}
                onImageUploaded={handleProfileImageUploaded}
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="이메일"
                value={profile.email}
                disabled
                margin="normal"
                variant="outlined"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="사용자 이름"
                name="username"
                value={profile.username}
                onChange={handleChange}
                disabled={!editMode}
                margin="normal"
                variant="outlined"
              />
            </Grid>

            <Grid item xs={12}>
              <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>
                주소 정보
              </Typography>
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="도시"
                name="city"
                value={profile.city || ""}
                onChange={handleChange}
                disabled={!editMode}
                margin="normal"
                variant="outlined"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="우편번호"
                name="zipcode"
                value={profile.zipcode || ""}
                onChange={handleChange}
                disabled={!editMode}
                margin="normal"
                variant="outlined"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="상세 주소"
                name="street"
                value={profile.street || ""}
                onChange={handleChange}
                disabled={!editMode}
                margin="normal"
                variant="outlined"
              />
            </Grid>
          </Grid>
        )}
      </Paper>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
      >
        <Alert
          onClose={handleCloseSnackbar}
          severity={snackbar.severity}
          sx={{ width: "100%" }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Container>
  );
};

export default Profile;
