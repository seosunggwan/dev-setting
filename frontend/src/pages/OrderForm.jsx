import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import axiosInstance from "../services/axiosInstance";
import { useLogin } from "../contexts/AuthContext";

const OrderForm = () => {
  const navigate = useNavigate();
  const { isLoggedIn, getAccessToken } = useLogin();

  const [members, setMembers] = useState([]);
  const [items, setItems]     = useState([]);
  const [selectedMember, setSelectedMember] = useState("");
  const [selectedItem, setSelectedItem]     = useState("");
  const [count, setCount]                 = useState(1);
  const [loading, setLoading]             = useState(true);
  const [error, setError]                 = useState(null);

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

        setMembers(Array.isArray(res.data.members) ? res.data.members : []);
        setItems(  Array.isArray(res.data.items)   ? res.data.items   : []);
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
        count 
      });
      navigate("/orders");  // 주문 목록으로
    } catch (e) {
      console.error("주문 생성 실패:", e);
      if (e.response?.status === 401) {
        alert("로그인이 필요합니다.");
        navigate("/login", { state: "/orders/form" });
      } else {
        setError("주문 생성에 실패했습니다.");
      }
    }
  };

  /* ― UI 렌더링 ― */
  if (loading) return <p>로딩 중...</p>;
  if (error)   return <p style={{ color: "red" }}>{error}</p>;

  return (
    <form onSubmit={handleSubmit}>
      <div>
        <label>회원 선택</label>
        <select
          value={selectedMember}
          onChange={(e) => setSelectedMember(e.target.value)}
          required
        >
          <option value="">-- 선택하세요 --</option>
          {members.map((m) => (
            <option key={m.id} value={m.id}>
              {m.username}
            </option>
          ))}
        </select>
      </div>

      <div>
        <label>상품 선택</label>
        <select
          value={selectedItem}
          onChange={(e) => setSelectedItem(e.target.value)}
          required
        >
          <option value="">-- 선택하세요 --</option>
          {items.map((i) => (
            <option key={i.id} value={i.id}>
              {i.name} (재고: {i.stockQuantity})
            </option>
          ))}
        </select>
      </div>

      <div>
        <label>수량</label>
        <input
          type="number"
          min="1"
          value={count}
          onChange={(e) => setCount(Number(e.target.value))}
          required
        />
      </div>

      <button type="submit">주문 생성</button>
    </form>
  );
};

export default OrderForm;
