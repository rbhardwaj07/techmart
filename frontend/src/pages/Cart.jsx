import { useState } from "react";
import { Link } from "react-router-dom";
import { formatPrice } from "../api.js";
import { useCart } from "../context/CartContext.jsx";

export default function Cart() {
  const { cart, loading, updateItem, removeItem } = useCart();
  const [error, setError] = useState("");

  async function run(action) {
    setError("");
    try {
      await action();
    } catch (err) {
      setError(err.message); // e.g. "Only 9 units of '...' are available"
    }
  }

  if (loading) return <p className="message">Loading your cart...</p>;

  if (cart.items.length === 0) {
    return (
      <div className="message">
        <h2>Your cart is empty</h2>
        <Link to="/">Browse products</Link>
      </div>
    );
  }

  return (
    <>
      <h1>Your cart</h1>
      {error && <p className="message error">{error}</p>}
      <div className="cart-list">
        {cart.items.map((item) => (
          <div className="cart-row" key={item.id}>
            <img src={item.imageUrl} alt={item.name} />
            <div className="cart-info">
              <Link to={`/products/${item.productId}`}>{item.name}</Link>
              <span className="muted">{formatPrice(item.unitPrice)} each</span>
            </div>
            <div className="qty">
              <button type="button" className="btn btn-outline btn-small" aria-label="Decrease quantity"
                      disabled={item.quantity <= 1}
                      onClick={() => run(() => updateItem(item.id, item.quantity - 1))}>-</button>
              <span>{item.quantity}</span>
              <button type="button" className="btn btn-outline btn-small" aria-label="Increase quantity"
                      disabled={item.quantity >= item.stock}
                      onClick={() => run(() => updateItem(item.id, item.quantity + 1))}>+</button>
            </div>
            <strong className="line-total">{formatPrice(item.lineTotal)}</strong>
            <button type="button" className="btn btn-outline btn-small"
                    onClick={() => run(() => removeItem(item.id))}>Remove</button>
          </div>
        ))}
      </div>
      <div className="cart-summary">
        <span>{cart.totalItems} {cart.totalItems === 1 ? "item" : "items"}</span>
        <strong>Total: {formatPrice(cart.total)}</strong>
      </div>
    </>
  );
}
