import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { fetchProduct, formatPrice } from "../api.js";

export default function ProductDetails() {
  const { id } = useParams();
  const [product, setProduct] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError("");
    fetchProduct(id)
      .then((data) => !cancelled && setProduct(data))
      .catch((err) => !cancelled && setError(err.message))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [id]);

  if (loading) return <p className="message">Loading product...</p>;

  if (error) {
    return (
      <div className="message error">
        <p>{error}</p>
        <Link to="/">Back to all products</Link>
      </div>
    );
  }

  const inStock = product.stock > 0;

  return (
    <>
      <Link to="/" className="back-link">
        &larr; Back to all products
      </Link>
      <section className="details">
        <div className="details-image">
          <img src={product.imageUrl} alt={product.name} />
        </div>
        <div className="details-info">
          <span className="badge">{product.category}</span>
          <h1>{product.name}</h1>
          <p className="details-price">{formatPrice(product.price)}</p>
          <p className="details-desc">{product.description}</p>
          <p className={inStock ? "stock in" : "stock out"}>
            {inStock ? `In stock (${product.stock} available)` : "Out of stock"}
          </p>
        </div>
      </section>
    </>
  );
}
