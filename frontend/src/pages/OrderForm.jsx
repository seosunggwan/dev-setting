import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import axiosInstance from "../services/axiosInstance";
import { useLogin } from "../contexts/AuthContext";
import {
  Container,
  Paper,
  Box,
  Typography,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
  Button,
  CircularProgress,
  Alert,
  Grid,
  Card,
  CardContent,
  Divider,
  Chip,
  Avatar,
} from "@mui/material";
import {
  ShoppingCart,
  Person,
  Inventory,
  Add,
  Remove,
  Send,
} from "@mui/icons-material";

const OrderForm = () => {
  const navigate = useNavigate();
  const { isLoggedIn, getAccessToken } = useLogin();

  const [members, setMembers] = useState([]);
  const [items, setItems] = useState([]);
  const [selectedMember, setSelectedMember] = useState("");
  const [selectedItem, setSelectedItem] = useState("");
  const [count, setCount] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // 선택된 상품 정보
  const selectedItemData = items.find(item => item.id === selectedItem);

  /* 주문 폼 데이터 호출 */
  useEffect(() => {
    if (!isLoggedIn) {
      alert("로그인이 필요합니다.");
      navigate("/login", { state: "/orders/form" });
      return;
    }

    const fetchData = async () => {
      try {
        setLoading(true);
        const token = getAccessToken();
        if (!token) {
          alert("로그인이 필요합니다.");
          navigate("/login", { state: "/orders/form" });
          return;
        }

        const res = await axiosInstance.get("/orders/form");

        console.log("API 응답 데이터:", res.data);
        console.log("회원 데이터:", res.data.members);
        console.log("상품 데이터:", res.data.items);

        setMembers(Array.isArray(res.data.members) ? res.data.members : []);
        setItems(Array.isArray(res.data.items) ? res.data.items : []);
      } catch (e) {
        console.error("데이터를 불러오는데 실패:", e);
        if (e.response?.status === 401) {
          alert("로그인이 필요합니다.");
          navigate("/login", { state: "/orders/form" });
        } else {
          setError("데이터를 불러오는데 실패했습니다.");
        }
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [isLoggedIn, navigate, getAccessToken]);

  /* 주문 생성 */
  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const token = getAccessToken();
      if (!token) {
        alert("로그인이 필요합니다.");
        navigate("/login", { state: "/orders/form" });
        return;
      }

      await axiosInstance.post("/orders", {
        memberId: selectedMember,
        itemId: selectedItem,
        count,
      });
      
      // 성공 알림
      alert("주문이 성공적으로 생성되었습니다!");
      navigate("/orders"); // 주문 목록으로
    } catch (e) {
      console.error("주문 생성 실패:", e);
      if (e.response?.status === 401) {
        alert("로그인이 필요합니다.");
        navigate("/login", { state: "/orders/form" });
      } else {
        setError("주문 생성에 실패했습니다.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  // 수량 증가/감소
  const handleCountChange = (increment) => {
    const newCount = count + increment;
    if (newCount >= 1 && (!selectedItemData || newCount <= selectedItemData.stockQuantity)) {
      setCount(newCount);
    }
  };

  /* ― UI 렌더링 ― */
  if (loading) {
    return (
      <Container maxWidth="sm">
        <Box
          display="flex"
          justifyContent="center"
          alignItems="center"
          minHeight="50vh"
          flexDirection="column"
        >
          <CircularProgress size={60} color="primary" />
          <Typography variant="h6" sx={{ mt: 2 }}>
            주문 정보를 불러오는 중...
          </Typography>
        </Box>
      </Container>
    );
  }

  return (
    <Container maxWidth="md" sx={{ py: 4 }}>
      <Paper elevation={3} sx={{ p: 4, borderRadius: 3 }}>
        {/* 헤더 */}
        <Box textAlign="center" mb={4}>
          <Avatar
            sx={{
              bgcolor: "primary.main",
              width: 64,
              height: 64,
              mx: "auto",
              mb: 2,
            }}
          >
            <ShoppingCart fontSize="large" />
          </Avatar>
          <Typography variant="h4" component="h1" gutterBottom color="primary">
            새 주문 생성
          </Typography>
          <Typography variant="subtitle1" color="text.secondary">
            원하시는 상품을 선택하고 주문을 생성해보세요
          </Typography>
        </Box>

        <Divider sx={{ mb: 4 }} />

        {/* 에러 메시지 */}
        {error && (
          <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        {/* 주문 폼 */}
        <Box component="form" onSubmit={handleSubmit}>
          <Grid container spacing={3}>
            {/* 회원 선택 */}
            <Grid item xs={12} md={6}>
              <Card variant="outlined" sx={{ height: "100%" }}>
                <CardContent>
                  <Box display="flex" alignItems="center" mb={2}>
                    <Person color="primary" sx={{ mr: 1 }} />
                    <Typography variant="h6">회원 선택</Typography>
                  </Box>
                  <FormControl fullWidth required>
                    <InputLabel>주문할 회원을 선택하세요</InputLabel>
                    <Select
                      value={selectedMember}
                      label="주문할 회원을 선택하세요"
                      onChange={(e) => setSelectedMember(e.target.value)}
                    >
                      {members.map((member) => {
                        // 안전한 이름 추출
                        const memberName = member.username || member.name || member.email || '사용자';
                        const initials = memberName && memberName.length > 0 ? memberName.charAt(0).toUpperCase() : 'U';
                        
                        return (
                          <MenuItem key={member.id} value={member.id}>
                            <Box display="flex" alignItems="center">
                              <Avatar sx={{ width: 24, height: 24, mr: 1, fontSize: 12 }}>
                                {initials}
                              </Avatar>
                              {memberName}
                            </Box>
                          </MenuItem>
                        );
                      })}
                    </Select>
                  </FormControl>
                </CardContent>
              </Card>
            </Grid>

            {/* 상품 선택 */}
            <Grid item xs={12} md={6}>
              <Card variant="outlined" sx={{ height: "100%" }}>
                <CardContent>
                  <Box display="flex" alignItems="center" mb={2}>
                    <Inventory color="primary" sx={{ mr: 1 }} />
                    <Typography variant="h6">상품 선택</Typography>
                  </Box>
                  <FormControl fullWidth required>
                    <InputLabel>주문할 상품을 선택하세요</InputLabel>
                    <Select
                      value={selectedItem}
                      label="주문할 상품을 선택하세요"
                      onChange={(e) => setSelectedItem(e.target.value)}
                    >
                      {items.map((item) => (
                        <MenuItem key={item.id} value={item.id}>
                          <Box display="flex" justifyContent="space-between" width="100%">
                            <Typography>{item.name}</Typography>
                            <Chip
                              label={`재고: ${item.stockQuantity}`}
                              size="small"
                              color={item.stockQuantity > 5 ? "success" : "warning"}
                              variant="outlined"
                            />
                          </Box>
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                  {selectedItemData && (
                    <Box mt={2} p={2} bgcolor="grey.100" borderRadius={1}>
                      <Typography variant="body2" color="text.secondary">
                        가격: ₩{selectedItemData.price?.toLocaleString() || "정보 없음"}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        재고: {selectedItemData.stockQuantity}개
                      </Typography>
                    </Box>
                  )}
                </CardContent>
              </Card>
            </Grid>

            {/* 수량 선택 */}
            <Grid item xs={12}>
              <Card variant="outlined">
                <CardContent>
                  <Typography variant="h6" mb={2}>
                    주문 수량
                  </Typography>
                  <Box display="flex" alignItems="center" justifyContent="center" gap={2}>
                    <Button
                      variant="outlined"
                      onClick={() => handleCountChange(-1)}
                      disabled={count <= 1}
                      sx={{ minWidth: 48, height: 48 }}
                    >
                      <Remove />
                    </Button>
                    <TextField
                      type="number"
                      value={count}
                      onChange={(e) => {
                        const newCount = Number(e.target.value);
                        if (newCount >= 1 && (!selectedItemData || newCount <= selectedItemData.stockQuantity)) {
                          setCount(newCount);
                        }
                      }}
                      inputProps={{
                        min: 1,
                        max: selectedItemData?.stockQuantity || 999,
                        style: { textAlign: "center" },
                      }}
                      sx={{ width: 120 }}
                    />
                    <Button
                      variant="outlined"
                      onClick={() => handleCountChange(1)}
                      disabled={selectedItemData && count >= selectedItemData.stockQuantity}
                      sx={{ minWidth: 48, height: 48 }}
                    >
                      <Add />
                    </Button>
                  </Box>
                  {selectedItemData && (
                    <Box textAlign="center" mt={2}>
                      <Typography variant="h6" color="primary">
                        총 금액: ₩{(selectedItemData.price * count).toLocaleString()}
                      </Typography>
                    </Box>
                  )}
                </CardContent>
              </Card>
            </Grid>

            {/* 주문 버튼 */}
            <Grid item xs={12}>
              <Box textAlign="center" mt={2}>
                <Button
                  type="submit"
                  variant="contained"
                  size="large"
                  disabled={!selectedMember || !selectedItem || submitting}
                  startIcon={submitting ? <CircularProgress size={20} /> : <Send />}
                  sx={{
                    px: 6,
                    py: 2,
                    fontSize: "1.1rem",
                    borderRadius: 3,
                    boxShadow: 3,
                    "&:hover": {
                      boxShadow: 6,
                    },
                  }}
                >
                  {submitting ? "주문 생성 중..." : "주문 생성"}
                </Button>
              </Box>
            </Grid>
          </Grid>
        </Box>
      </Paper>
    </Container>
  );
};

export default OrderForm;
