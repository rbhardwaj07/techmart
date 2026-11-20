import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { addCartItem, fetchCart, removeCartItem, updateCartItem } from "../api.js";
import { useAuth } from "./AuthContext.jsx";

const EMPTY_CART = { items: [], totalItems: 0, total: 0 };
const CartContext = createContext(null);

export function CartProvider({ children }) {
  const { user } = useAuth();
  const [cart, setCart] = useState(EMPTY_CART);
  const [loading, setLoading] = useState(false);

  // Load the cart from the backend whenever the logged-in user changes
  useEffect(() => {
    if (!user) {
      setCart(EMPTY_CART);
      return;
    }
    setLoading(true);
    fetchCart()
      .then(setCart)
      .catch(() => setCart(EMPTY_CART))
      .finally(() => setLoading(false));
  }, [user]);

  // Every cart call returns the updated cart, so we simply store it.
  const addItem = useCallback(async (productId, quantity = 1) => setCart(await addCartItem(productId, quantity)), []);
  const updateItem = useCallback(async (itemId, quantity) => setCart(await updateCartItem(itemId, quantity)), []);
  const removeItem = useCallback(async (itemId) => setCart(await removeCartItem(itemId)), []);

  return (
    <CartContext.Provider value={{ cart, loading, addItem, updateItem, removeItem }}>
      {children}
    </CartContext.Provider>
  );
}

export const useCart = () => useContext(CartContext);
