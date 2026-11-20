// All communication with the backend goes through these functions.
// The frontend never talks to the database directly.

const TOKEN_KEY = "techmart_token";

export const getToken = () => localStorage.getItem(TOKEN_KEY);
export const setToken = (token) => localStorage.setItem(TOKEN_KEY, token);
export const clearToken = () => localStorage.removeItem(TOKEN_KEY);

async function request(url, { method = "GET", body, auth = false } = {}) {
  const headers = {};
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const token = getToken();
  if (auth && token) headers.Authorization = `Bearer ${token}`;

  const response = await fetch(url, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const data = await response.json();
      if (data.message) message = data.message;
    } catch {
      /* response had no JSON body */
    }
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }
  return response.json();
}

// ---- Part 1: products (public) ----
export function fetchProducts({ search, categoryId, minPrice, maxPrice, sort } = {}) {
  const params = new URLSearchParams();
  if (search) params.set("search", search);
  if (categoryId) params.set("categoryId", categoryId);
  if (minPrice !== "" && minPrice != null) params.set("minPrice", minPrice);
  if (maxPrice !== "" && maxPrice != null) params.set("maxPrice", maxPrice);
  if (sort) params.set("sort", sort);
  const query = params.toString();
  return request(`/api/products${query ? `?${query}` : ""}`);
}

export function fetchProduct(id) {
  return request(`/api/products/${id}`);
}

export function fetchCategories() {
  return request("/api/categories");
}

// ---- Part 2: authentication ----
export const registerUser = (name, email, password) =>
  request("/api/auth/register", { method: "POST", body: { name, email, password } });

export const loginUser = (email, password) =>
  request("/api/auth/login", { method: "POST", body: { email, password } });

export const fetchMe = () => request("/api/auth/me", { auth: true });

// ---- Part 2: cart (needs login) ----
export const fetchCart = () => request("/api/cart", { auth: true });

export const addCartItem = (productId, quantity) =>
  request("/api/cart/items", { method: "POST", body: { productId, quantity }, auth: true });

export const updateCartItem = (itemId, quantity) =>
  request(`/api/cart/items/${itemId}`, { method: "PUT", body: { quantity }, auth: true });

export const removeCartItem = (itemId) =>
  request(`/api/cart/items/${itemId}`, { method: "DELETE", auth: true });

export function formatPrice(value) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(value);
}
