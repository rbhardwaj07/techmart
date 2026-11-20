import { Link, Route, Routes } from "react-router-dom";
import ProductList from "./pages/ProductList.jsx";
import ProductDetails from "./pages/ProductDetails.jsx";
import Login from "./pages/Login.jsx";
import Register from "./pages/Register.jsx";
import Cart from "./pages/Cart.jsx";
import ProtectedRoute from "./components/ProtectedRoute.jsx";
import { useAuth } from "./context/AuthContext.jsx";
import { useCart } from "./context/CartContext.jsx";

export default function App() {
  const { user, logout } = useAuth();
  const { cart } = useCart();

  return (
    <>
      <header className="site-header">
        <div className="container header-inner">
          <Link to="/" className="logo">
            Tech<span>Mart</span>
          </Link>
          <p className="tagline">Electronic gadgets for everyday life</p>
          <nav className="nav">
            {user ? (
              <>
                <span className="nav-user">Hi, {user.name}</span>
                <Link to="/cart" className="nav-link">
                  Cart{cart.totalItems > 0 && <span className="cart-badge">{cart.totalItems}</span>}
                </Link>
                <button type="button" className="btn btn-outline btn-small" onClick={logout}>
                  Log out
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="nav-link">Log in</Link>
                <Link to="/register" className="btn btn-small">Register</Link>
              </>
            )}
          </nav>
        </div>
      </header>

      <main className="container">
        <Routes>
          <Route path="/" element={<ProductList />} />
          <Route path="/products/:id" element={<ProductDetails />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/cart" element={<ProtectedRoute><Cart /></ProtectedRoute>} />
          <Route
            path="*"
            element={
              <div className="message">
                <h2>Page not found</h2>
                <Link to="/">Back to all products</Link>
              </div>
            }
          />
        </Routes>
      </main>

      <footer className="site-footer">TechMart - Lab Assignment, Parts 1 and 2</footer>
    </>
  );
}
