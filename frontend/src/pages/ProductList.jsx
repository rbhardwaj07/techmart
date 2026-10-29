import { useEffect, useState } from "react";
import { fetchCategories, fetchProducts } from "../api.js";
import ProductCard from "../components/ProductCard.jsx";

const EMPTY_FILTERS = { search: "", categoryId: "", minPrice: "", maxPrice: "", sort: "" };

export default function ProductList() {
  const [categories, setCategories] = useState([]);
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Load the category dropdown once
  useEffect(() => {
    fetchCategories()
      .then(setCategories)
      .catch(() => setCategories([]));
  }, []);

  // Reload products whenever a filter changes (search text is debounced)
  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    const timer = setTimeout(() => {
      fetchProducts(filters)
        .then((data) => {
          if (cancelled) return;
          setProducts(data);
          setError("");
        })
        .catch((err) => {
          if (cancelled) return;
          setProducts([]);
          setError(err.message);
        })
        .finally(() => {
          if (!cancelled) setLoading(false);
        });
    }, 250);

    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [filters]);

  const update = (field) => (e) => setFilters({ ...filters, [field]: e.target.value });

  return (
    <>
      <section className="filters" aria-label="Search and filter products">
        <input
          type="search"
          placeholder="Search gadgets..."
          value={filters.search}
          onChange={update("search")}
          className="search-input"
        />
        <select value={filters.categoryId} onChange={update("categoryId")}>
          <option value="">All categories</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        <input
          type="number"
          min="0"
          placeholder="Min price"
          value={filters.minPrice}
          onChange={update("minPrice")}
          className="price-input"
        />
        <input
          type="number"
          min="0"
          placeholder="Max price"
          value={filters.maxPrice}
          onChange={update("maxPrice")}
          className="price-input"
        />
        <select value={filters.sort} onChange={update("sort")}>
          <option value="">Sort: Default</option>
          <option value="name">Name (A-Z)</option>
          <option value="price_asc">Price: Low to High</option>
          <option value="price_desc">Price: High to Low</option>
        </select>
        <button type="button" className="btn btn-outline" onClick={() => setFilters(EMPTY_FILTERS)}>
          Clear
        </button>
      </section>

      {error && <p className="message error">{error}</p>}
      {loading && !error && <p className="message">Loading products...</p>}

      {!loading && !error && (
        <>
          <p className="result-count">
            {products.length} {products.length === 1 ? "product" : "products"} found
          </p>
          {products.length === 0 ? (
            <p className="message">No products match your search.</p>
          ) : (
            <div className="grid">
              {products.map((p) => (
                <ProductCard key={p.id} product={p} />
              ))}
            </div>
          )}
        </>
      )}
    </>
  );
}
