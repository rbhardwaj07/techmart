import { Link, Route, Routes } from "react-router-dom";
import ProductList from "./pages/ProductList.jsx";
import ProductDetails from "./pages/ProductDetails.jsx";

export default function App() {
  return (
    <>
      <header className="site-header">
        <div className="container header-inner">
          <Link to="/" className="logo">
            Tech<span>Mart</span>
          </Link>
          <p className="tagline">Electronic gadgets for everyday life</p>
        </div>
      </header>

      <main className="container">
        <Routes>
          <Route path="/" element={<ProductList />} />
          <Route path="/products/:id" element={<ProductDetails />} />
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

      <footer className="site-footer">TechMart - Lab Assignment, Part 1</footer>
    </>
  );
}
