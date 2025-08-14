import React, { useState, useEffect } from "react";
import axiosInstance from "../services/axiosInstance";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useLogin } from "../contexts/AuthContext";

const OrderList = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [orderSearch, setOrderSearch] = useState({
    memberName: "",
    orderStatus: null,
    itemName: "",
    categoryName: "",
    orderDateFrom: "",
    orderDateTo: "",
    minPrice: "",
    maxPrice: "",
  });
  const [pageInfo, setPageInfo] = useState({
    page: 0,
    size: 10,
    total: 0,
    totalPages: 0,
    hasNext: false,
    hasPrevious: false,
  });
  const [searchParams, setSearchParams] = useSearchParams();
  const { isLoggedIn, logout, isAdmin, getUserRole } = useLogin();
  const navigate = useNavigate();

  // URL에서 파라미터 가져오기
  const page = parseInt(searchParams.get("page") || "0");
  const size = parseInt(searchParams.get("size") || "10");
  const urlMemberName = searchParams.get("memberName") || "";
  const urlOrderStatus = searchParams.get("orderStatus") || "";
  const urlItemName = searchParams.get("itemName") || "";
  const urlCategoryName = searchParams.get("categoryName") || "";
  const urlOrderDateFrom = searchParams.get("orderDateFrom") || "";
  const urlOrderDateTo = searchParams.get("orderDateTo") || "";
  const urlMinPrice = searchParams.get("minPrice") || "";
  const urlMaxPrice = searchParams.get("maxPrice") || "";

  // 로그인 상태 체크
  useEffect(() => {
    if (!isLoggedIn) {
      alert("로그인이 필요합니다.");
      navigate("/login", { state: "/orders" });
      return;
    }
  }, [isLoggedIn, navigate]);

  // URL 파라미터 변경 시 검색 조건 업데이트 및 데이터 로드
  useEffect(() => {
    if (!isLoggedIn) return; // 로그인되지 않았으면 실행하지 않음

    // URL 파라미터로부터 검색 조건 업데이트
    setOrderSearch({
      memberName: urlMemberName,
      orderStatus: urlOrderStatus || null,
      itemName: urlItemName,
      categoryName: urlCategoryName,
      orderDateFrom: urlOrderDateFrom,
      orderDateTo: urlOrderDateTo,
      minPrice: urlMinPrice,
      maxPrice: urlMaxPrice,
    });

    // URL의 페이지 정보로 주문 목록 조회
    fetchOrders(page, size, urlMemberName, urlOrderStatus, urlItemName, urlCategoryName, urlOrderDateFrom, urlOrderDateTo, urlMinPrice, urlMaxPrice);
  }, [isLoggedIn, page, size, urlMemberName, urlOrderStatus, urlItemName, urlCategoryName, urlOrderDateFrom, urlOrderDateTo, urlMinPrice, urlMaxPrice]);

  const fetchOrders = async (page, size, memberName = "", orderStatus = "", itemName = "", categoryName = "", orderDateFrom = "", orderDateTo = "", minPrice = "", maxPrice = "") => {
    try {
      setLoading(true);
      console.log("📡 fetchOrders 호출됨!");
      console.log("검색 조건:", {
        memberName: memberName || "",
        orderStatus: orderStatus || "",
        itemName: itemName || "",
        categoryName: categoryName || "",
        orderDateFrom: orderDateFrom || "",
        orderDateTo: orderDateTo || "",
        minPrice: minPrice || "",
        maxPrice: maxPrice || "",
        page: page,
        size: size,
      });

      // 페이지네이션 API 엔드포인트 사용
      let url = "/orders/search/page";
      const params = {
        page: page,
        size: size,
      };

      // 빈 문자열이 아닌 경우에만 파라미터로 추가
      if (memberName && memberName.trim() !== "") {
        params.memberName = memberName.trim();
      }

      if (orderStatus) {
        params.orderStatus = orderStatus;
      }

      if (itemName && itemName.trim() !== "") {
        params.itemName = itemName.trim();
      }

      if (categoryName && categoryName.trim() !== "") {
        params.categoryName = categoryName.trim();
        console.log("🔍 카테고리 검색 파라미터 추가:", categoryName.trim());
      }

      if (orderDateFrom && orderDateFrom.trim() !== "") {
        // datetime-local 형식을 ISO 형식으로 변환
        const fromDate = new Date(orderDateFrom.trim());
        params.orderDateFrom = fromDate.toISOString();
      }

      if (orderDateTo && orderDateTo.trim() !== "") {
        // datetime-local 형식을 ISO 형식으로 변환
        const toDate = new Date(orderDateTo.trim());
        params.orderDateTo = toDate.toISOString();
      }

      if (minPrice && minPrice.trim() !== "") {
        params.minPrice = parseInt(minPrice);
      }

      if (maxPrice && maxPrice.trim() !== "") {
        params.maxPrice = parseInt(maxPrice);
      }
      
      console.log("API 파라미터:", params);

      // 검색 API 호출
      const response = await axiosInstance.get(url, {
        params: params,
      });

      console.log("응답 데이터:", response.data);

      // 페이지네이션 응답 형식 처리
      if (response.data && response.data.orders) {
        setOrders(response.data.orders);
        setPageInfo(response.data.pageInfo);
      } else {
        // 기존 형식 지원 (백엔드가 배열 형태로 응답하는 경우)
        setOrders(Array.isArray(response.data) ? response.data : []);
      }
    } catch (error) {
      console.error("주문 목록을 불러오는데 실패했습니다:", error);
      console.error("에러 상세:", {
        status: error.response?.status,
        data: error.response?.data,
        headers: error.response?.headers,
        message: error.message,
      });

      if (error.response?.status === 401) {
        console.log(
          "인증 토큰이 만료되었거나 유효하지 않습니다. 재로그인 필요"
        );
        alert("로그인 세션이 만료되었습니다. 다시 로그인해주세요.");
        // 토큰 만료 시 로그아웃 처리
        logout();
        navigate("/login", { state: "/orders" });
      } else {
        setError(
          "주문 목록을 불러오는데 실패했습니다: " +
            (error.response?.data || error.message)
        );
      }
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    console.log("🔍 검색 버튼 클릭됨!");
    console.log("검색 조건:", orderSearch);
    
    // 검색 시 첫 페이지로 이동 (검색 조건 포함)
    const params = { 
      page: 0, 
      size: pageInfo.size
    };
    
    // 검색 조건이 있으면 URL 파라미터에 추가
    if (orderSearch.memberName && orderSearch.memberName.trim() !== "") {
      params.memberName = orderSearch.memberName.trim();
    }
    
    if (orderSearch.orderStatus) {
      params.orderStatus = orderSearch.orderStatus;
    }

    if (orderSearch.itemName && orderSearch.itemName.trim() !== "") {
      params.itemName = orderSearch.itemName.trim();
    }

    if (orderSearch.categoryName && orderSearch.categoryName.trim() !== "") {
      params.categoryName = orderSearch.categoryName.trim();
    }

    if (orderSearch.orderDateFrom && orderSearch.orderDateFrom.trim() !== "") {
      // datetime-local 형식을 ISO 형식으로 변환
      const fromDate = new Date(orderSearch.orderDateFrom.trim());
      params.orderDateFrom = fromDate.toISOString();
    }

    if (orderSearch.orderDateTo && orderSearch.orderDateTo.trim() !== "") {
      // datetime-local 형식을 ISO 형식으로 변환
      const toDate = new Date(orderSearch.orderDateTo.trim());
      params.orderDateTo = toDate.toISOString();
    }

    if (orderSearch.minPrice && orderSearch.minPrice.trim() !== "") {
      params.minPrice = orderSearch.minPrice.trim();
    }

    if (orderSearch.maxPrice && orderSearch.maxPrice.trim() !== "") {
      params.maxPrice = orderSearch.maxPrice.trim();
    }
    
    console.log("URL 파라미터:", params);
    setSearchParams(params);
  };

  const handlePageChange = (newPage) => {
    // URL 파라미터 업데이트
    setSearchParams({
      page: newPage,
      size: pageInfo.size,
      ...(orderSearch.memberName && { memberName: orderSearch.memberName }),
      ...(orderSearch.orderStatus && { orderStatus: orderSearch.orderStatus }),
      ...(orderSearch.itemName && { itemName: orderSearch.itemName }),
      ...(orderSearch.categoryName && { categoryName: orderSearch.categoryName }),
      ...(orderSearch.orderDateFrom && { orderDateFrom: orderSearch.orderDateFrom }),
      ...(orderSearch.orderDateTo && { orderDateTo: orderSearch.orderDateTo }),
      ...(orderSearch.minPrice && { minPrice: orderSearch.minPrice }),
      ...(orderSearch.maxPrice && { maxPrice: orderSearch.maxPrice }),
    });
  };

  const handleReset = () => {
    setOrderSearch({
      memberName: "",
      orderStatus: null,
      itemName: "",
      categoryName: "",
      orderDateFrom: "",
      orderDateTo: "",
      minPrice: "",
      maxPrice: "",
    });
    setSearchParams({ page: 0, size: pageInfo.size });
  };

  // 페이지네이션 컴포넌트
  const Pagination = () => {
    const pageButtons = [];
    const maxButtonCount = 5; // 표시할 최대 페이지 버튼 수

    // 현재 페이지 주변 버튼만 표시하기 위한 시작/끝 계산
    let startPage = Math.max(0, pageInfo.page - Math.floor(maxButtonCount / 2));
    let endPage = Math.min(
      pageInfo.totalPages - 1,
      startPage + maxButtonCount - 1
    );

    // 최대 버튼 수에 맞게 시작 페이지 조정
    if (endPage - startPage + 1 < maxButtonCount) {
      startPage = Math.max(0, endPage - maxButtonCount + 1);
    }

    // 이전 페이지 버튼
    pageButtons.push(
      <button
        key="prev"
        onClick={() => handlePageChange(pageInfo.page - 1)}
        disabled={!pageInfo.hasPrevious}
        style={{
          padding: "5px 10px",
          margin: "0 5px",
          backgroundColor: !pageInfo.hasPrevious ? "#e5e7eb" : "#f3f4f6",
          border: "1px solid #d1d5db",
          borderRadius: "4px",
          cursor: pageInfo.hasPrevious ? "pointer" : "not-allowed",
        }}
      >
        이전
      </button>
    );

    // 페이지 번호 버튼들
    for (let i = startPage; i <= endPage; i++) {
      pageButtons.push(
        <button
          key={i}
          onClick={() => handlePageChange(i)}
          style={{
            padding: "5px 10px",
            margin: "0 5px",
            backgroundColor: i === pageInfo.page ? "#4F46E5" : "#f3f4f6",
            color: i === pageInfo.page ? "white" : "black",
            border: "1px solid #d1d5db",
            borderRadius: "4px",
            cursor: "pointer",
          }}
        >
          {i + 1}
        </button>
      );
    }

    // 다음 페이지 버튼
    pageButtons.push(
      <button
        key="next"
        onClick={() => handlePageChange(pageInfo.page + 1)}
        disabled={!pageInfo.hasNext}
        style={{
          padding: "5px 10px",
          margin: "0 5px",
          backgroundColor: !pageInfo.hasNext ? "#e5e7eb" : "#f3f4f6",
          border: "1px solid #d1d5db",
          borderRadius: "4px",
          cursor: pageInfo.hasNext ? "pointer" : "not-allowed",
        }}
      >
        다음
      </button>
    );

    return (
      <div
        style={{
          display: "flex",
          justifyContent: "center",
          marginTop: "20px",
          padding: "10px",
        }}
      >
        {pageButtons}
      </div>
    );
  };

  const handleCancelOrder = async (orderId) => {
    try {
      await axiosInstance.post(`/orders/${orderId}/cancel`, {});

      alert("주문이 취소되었습니다.");
      fetchOrders(page, size, urlMemberName, urlOrderStatus, urlItemName, urlCategoryName, urlOrderDateFrom, urlOrderDateTo, urlMinPrice, urlMaxPrice);
    } catch (error) {
      console.error("주문 취소에 실패했습니다:", error);
      if (error.response?.status === 401) {
        alert("로그인이 필요합니다.");
        navigate("/login", { state: "/orders" });
      } else {
        alert("주문 취소에 실패했습니다.");
      }
    }
  };

  const formatDate = (dateStr) => {
    try {
      if (!dateStr) return "날짜 정보 없음";

      // 숫자 문자열이 밀리초 타임스탬프인 경우
      const timestamp = Number(dateStr);
      if (!isNaN(timestamp) && String(timestamp).length > 10) {
        const date = new Date(timestamp);
        if (date.toString() !== "Invalid Date") {
          return date.toLocaleString("ko-KR", {
            year: "numeric",
            month: "long",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit",
            hour12: true,
          });
        }
      }

      // ISO 형식 문자열인 경우
      const date = new Date(dateStr);
      if (date.toString() !== "Invalid Date") {
        return date.toLocaleString("ko-KR", {
          year: "numeric",
          month: "long",
          day: "numeric",
          hour: "2-digit",
          minute: "2-digit",
          hour12: true,
        });
      }

      return dateStr;
    } catch (e) {
      console.error("날짜 변환 오류:", e);
      return dateStr;
    }
  };

  if (loading) {
    return (
      <div style={{ textAlign: "center", marginTop: "20px" }}>로딩 중...</div>
    );
  }

  if (error) {
    return (
      <div style={{ textAlign: "center", marginTop: "20px" }}>
        <p>{error}</p>
        <button
          onClick={() => window.location.reload()}
          style={{
            padding: "8px 16px",
            backgroundColor: "#4F46E5",
            color: "white",
            border: "none",
            borderRadius: "4px",
            marginTop: "10px",
            cursor: "pointer",
          }}
        >
          다시 시도
        </button>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: "1200px", margin: "0 auto", padding: "20px" }}>
      <h1
        style={{ fontSize: "24px", fontWeight: "bold", marginBottom: "20px" }}
      >
        주문 목록
      </h1>

      <form
        onSubmit={handleSearch}
        style={{
          marginBottom: "20px",
          padding: "20px",
          backgroundColor: "#f9fafb",
          borderRadius: "8px",
          border: "1px solid #eee",
        }}
      >
        {/* 사용자 역할 표시 */}
        <div style={{ marginBottom: "20px", fontSize: "14px", color: "#666" }}>
          {isAdmin() ? (
            <span style={{ color: "#ef4444", fontWeight: "bold" }}>
              🔧 관리자 모드: 모든 사용자의 주문을 조회할 수 있습니다
            </span>
          ) : (
            <span style={{ color: "#10b981", fontWeight: "bold" }}>
              👤 사용자 모드: 본인의 주문만 조회됩니다
            </span>
          )}
        </div>
        
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(250px, 1fr))",
            gap: "15px",
            marginBottom: "15px",
          }}
        >
          {/* 관리자만 회원명 검색 필드 표시 */}
          {isAdmin() && (
            <div>
              <label
                style={{
                  display: "block",
                  marginBottom: "6px",
                  fontWeight: "500",
                }}
              >
                회원명
              </label>
              <input
                type="text"
                value={orderSearch.memberName}
                onChange={(e) =>
                  setOrderSearch({
                    ...orderSearch,
                    memberName: e.target.value,
                  })
                }
                placeholder="회원 이름 입력"
                style={{
                  width: "100%",
                  padding: "8px",
                  border: "1px solid #ddd",
                  borderRadius: "4px",
                }}
              />
            </div>
          )}
          
          <div>
            <label
              style={{
                display: "block",
                marginBottom: "6px",
                fontWeight: "500",
              }}
            >
              주문상태
            </label>
            <select
              value={orderSearch.orderStatus || ""}
              onChange={(e) =>
                setOrderSearch({
                  ...orderSearch,
                  orderStatus: e.target.value || null,
                })
              }
              style={{
                width: "100%",
                padding: "8px",
                border: "1px solid #ddd",
                borderRadius: "4px",
                backgroundColor: "white",
              }}
            >
              <option value="">전체</option>
              <option value="ORDER">주문</option>
              <option value="CANCEL">취소</option>
            </select>
          </div>

          <div>
            <label
              style={{
                display: "block",
                marginBottom: "6px",
                fontWeight: "500",
              }}
            >
              상품명
            </label>
            <input
              type="text"
              value={orderSearch.itemName}
              onChange={(e) =>
                setOrderSearch({
                  ...orderSearch,
                  itemName: e.target.value,
                })
              }
              placeholder="상품명 입력"
              style={{
                width: "100%",
                padding: "8px",
                border: "1px solid #ddd",
                borderRadius: "4px",
              }}
            />
          </div>

          <div>
            <label
              style={{
                display: "block",
                marginBottom: "6px",
                fontWeight: "500",
              }}
            >
              카테고리
            </label>
            <select
              value={orderSearch.categoryName || ""}
              onChange={(e) =>
                setOrderSearch({
                  ...orderSearch,
                  categoryName: e.target.value || "",
                })
              }
              style={{
                width: "100%",
                padding: "8px",
                border: "1px solid #ddd",
                borderRadius: "4px",
                backgroundColor: "white",
              }}
            >
              <option value="">전체</option>
              <option value="음반">음반</option>
              <option value="도서">도서</option>
              <option value="영화">영화</option>
            </select>
          </div>

          <div>
            <label
              style={{
                display: "block",
                marginBottom: "6px",
                fontWeight: "500",
              }}
            >
              주문일시 시작
            </label>
            <input
              type="datetime-local"
              value={orderSearch.orderDateFrom}
              onChange={(e) =>
                setOrderSearch({
                  ...orderSearch,
                  orderDateFrom: e.target.value,
                })
              }
              style={{
                width: "100%",
                padding: "8px",
                border: "1px solid #ddd",
                borderRadius: "4px",
              }}
            />
          </div>

          <div>
            <label
              style={{
                display: "block",
                marginBottom: "6px",
                fontWeight: "500",
              }}
            >
              주문일시 끝
            </label>
            <input
              type="datetime-local"
              value={orderSearch.orderDateTo}
              onChange={(e) =>
                setOrderSearch({
                  ...orderSearch,
                  orderDateTo: e.target.value,
                })
              }
              style={{
                width: "100%",
                padding: "8px",
                border: "1px solid #ddd",
                borderRadius: "4px",
              }}
            />
          </div>

          <div>
            <label
              style={{
                display: "block",
                marginBottom: "6px",
                fontWeight: "500",
              }}
            >
              최소 금액
            </label>
            <input
              type="number"
              value={orderSearch.minPrice}
              onChange={(e) =>
                setOrderSearch({
                  ...orderSearch,
                  minPrice: e.target.value,
                })
              }
              placeholder="최소 금액"
              min="0"
              style={{
                width: "100%",
                padding: "8px",
                border: "1px solid #ddd",
                borderRadius: "4px",
              }}
            />
          </div>

          <div>
            <label
              style={{
                display: "block",
                marginBottom: "6px",
                fontWeight: "500",
              }}
            >
              최대 금액
            </label>
            <input
              type="number"
              value={orderSearch.maxPrice}
              onChange={(e) =>
                setOrderSearch({
                  ...orderSearch,
                  maxPrice: e.target.value,
                })
              }
              placeholder="최대 금액"
              min="0"
              style={{
                width: "100%",
                padding: "8px",
                border: "1px solid #ddd",
                borderRadius: "4px",
              }}
            />
          </div>
        </div>

        <div style={{ display: "flex", gap: "10px", justifyContent: "center" }}>
          <button
            type="submit"
            style={{
              padding: "10px 20px",
              backgroundColor: "#4F46E5",
              color: "white",
              border: "none",
              borderRadius: "4px",
              cursor: "pointer",
              fontWeight: "500",
            }}
          >
            🔍 검색
          </button>
          <button
            type="button"
            onClick={handleReset}
            style={{
              padding: "10px 20px",
              backgroundColor: "#6B7280",
              color: "white",
              border: "none",
              borderRadius: "4px",
              cursor: "pointer",
              fontWeight: "500",
            }}
          >
            🔄 초기화
          </button>
        </div>
      </form>

      <div style={{ overflowX: "auto" }}>
        <table
          style={{
            width: "100%",
            borderCollapse: "collapse",
            marginTop: "10px",
          }}
        >
          <thead>
            <tr
              style={{
                backgroundColor: "#f3f4f6",
                borderBottom: "1px solid #e5e7eb",
              }}
            >
              <th
                style={{
                  padding: "10px",
                  textAlign: "left",
                  fontWeight: "500",
                }}
              >
                주문번호
              </th>
              <th
                style={{
                  padding: "10px",
                  textAlign: "left",
                  fontWeight: "500",
                }}
              >
                회원명
              </th>
              <th
                style={{
                  padding: "10px",
                  textAlign: "left",
                  fontWeight: "500",
                }}
              >
                주문상태
              </th>
              <th
                style={{
                  padding: "10px",
                  textAlign: "left",
                  fontWeight: "500",
                }}
              >
                상품 정보
              </th>
              <th
                style={{
                  padding: "10px",
                  textAlign: "left",
                  fontWeight: "500",
                }}
              >
                주문일시
              </th>
              <th
                style={{
                  padding: "10px",
                  textAlign: "left",
                  fontWeight: "500",
                }}
              >
                주문금액
              </th>
              <th
                style={{
                  padding: "10px",
                  textAlign: "left",
                  fontWeight: "500",
                }}
              >
                관리
              </th>
            </tr>
          </thead>
          <tbody>
            {orders.length > 0 ? (
              orders.map((order) => (
                <tr
                  key={order.orderId}
                  style={{ borderBottom: "1px solid #e5e7eb" }}
                >
                  <td style={{ padding: "12px 10px" }}>{order.orderId}</td>
                  <td style={{ padding: "12px 10px" }}>{order.memberName}</td>
                  <td style={{ padding: "12px 10px" }}>
                    <span
                      style={{
                        display: "inline-block",
                        padding: "2px 8px",
                        borderRadius: "12px",
                        fontSize: "14px",
                        backgroundColor:
                          order.orderStatus === "ORDER" ? "#dcfce7" : "#fee2e2",
                        color:
                          order.orderStatus === "ORDER" ? "#166534" : "#991b1b",
                      }}
                    >
                      {order.orderStatus === "ORDER" ? "주문" : "취소"}
                    </span>
                  </td>
                  <td style={{ padding: "12px 10px" }}>
                    {order.orderItems.map((item, index) => (
                      <div key={index} style={{ marginBottom: index === order.orderItems.length - 1 ? "0" : "4px" }}>
                        <div style={{ fontSize: "14px", fontWeight: "500" }}>
                          {item.itemName}
                        </div>
                        <div style={{ fontSize: "12px", color: "#666" }}>
                          <span
                            style={{
                              display: "inline-block",
                              padding: "1px 6px",
                              borderRadius: "8px",
                              fontSize: "11px",
                              backgroundColor: 
                                item.itemType === "BOOK" ? "#dbeafe" :
                                item.itemType === "ALBUM" ? "#fed7d7" :
                                item.itemType === "MOVIE" ? "#d1fae5" : "#f3f4f6",
                              color:
                                item.itemType === "BOOK" ? "#1e40af" :
                                item.itemType === "ALBUM" ? "#991b1b" :
                                item.itemType === "MOVIE" ? "#065f46" : "#374151",
                            }}
                          >
                            {item.itemTypeDisplay || "기타"}
                          </span>
                          <span style={{ marginLeft: "6px" }}>
                            {item.count}개 × {item.orderPrice.toLocaleString()}원
                          </span>
                        </div>
                      </div>
                    ))}
                  </td>
                  <td style={{ padding: "12px 10px" }}>
                    {formatDate(order.orderDate)}
                  </td>
                  <td style={{ padding: "12px 10px", fontWeight: "500" }}>
                    {order.orderItems
                      .reduce(
                        (total, item) => total + item.orderPrice * item.count,
                        0
                      )
                      .toLocaleString()}
                    원
                  </td>
                  <td style={{ padding: "12px 10px" }}>
                    {order.orderStatus === "ORDER" && (
                      <button
                        onClick={() => handleCancelOrder(order.orderId)}
                        style={{
                          backgroundColor: "transparent",
                          color: "#ef4444",
                          border: "none",
                          cursor: "pointer",
                          fontSize: "14px",
                          fontWeight: "500",
                        }}
                      >
                        취소
                      </button>
                    )}
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td
                  colSpan="7"
                  style={{ padding: "40px 10px", textAlign: "center" }}
                >
                  <div>
                    <p style={{ fontSize: "16px", marginBottom: "8px" }}>
                      주문 내역이 없습니다.
                    </p>
                    <p style={{ fontSize: "14px", color: "#6b7280" }}>
                      새로운 주문을 생성해보세요.
                    </p>
                  </div>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* 페이지네이션 컴포넌트 추가 */}
      {orders.length > 0 && <Pagination />}
    </div>
  );
};

export default OrderList;
