import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { useCart } from "../context/CartContext.jsx";

/** Adds one unit of a product to the cart (or sends a logged-out visitor to the login page). */
export default function AddToCartButton({ product, quantity = 1, className = "btn btn-small", onResult }) {
  const { user } = useAuth();
  const { addItem } = useCart();
  const navigate = useNavigate();
  const location = useLocation();
  const [busy, setBusy] = useState(false);

  async function handleClick() {
    if (!user) {
      navigate("/login", { state: { from: location.pathname } });
      return;
    }
    setBusy(true);
    try {
      await addItem(product.id, quantity);
      onResult?.({ ok: true, message: `${product.name} added to cart` });
    } catch (err) {
      onResult?.({ ok: false, message: err.message });
    } finally {
      setBusy(false);
    }
  }

  const outOfStock = product.stock <= 0;
  return (
    <button type="button" className={className} onClick={handleClick} disabled={busy || outOfStock}>
      {outOfStock ? "Out of stock" : busy ? "Adding..." : "Add to cart"}
    </button>
  );
}
