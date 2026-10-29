// All communication with the backend goes through these functions.
// The frontend never talks to the database directly.

async function request(url) {
  const response = await fetch(url);
  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = await response.json();
      if (body.message) message = body.message;
    } catch {
      /* response had no JSON body */
    }
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }
  return response.json();
}

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

export function formatPrice(value) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(value);
}
