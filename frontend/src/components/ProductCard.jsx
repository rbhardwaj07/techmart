import { Link } from "react-router-dom";
import { formatPrice } from "../api.js";

export default function ProductCard({ product }) {
  return (
    <article className="card">
      <Link to={`/products/${product.id}`} className="card-image">
        <img src={product.imageUrl} alt={product.name} loading="lazy" />
      </Link>
      <div className="card-body">
        <span className="badge">{product.category}</span>
        <h3 className="card-title">
          <Link to={`/products/${product.id}`}>{product.name}</Link>
        </h3>
        <p className="card-desc">{product.description}</p>
        <div className="card-footer">
          <strong className="price">{formatPrice(product.price)}</strong>
          <Link to={`/products/${product.id}`} className="btn btn-small">
            View details
          </Link>
        </div>
      </div>
    </article>
  );
}
