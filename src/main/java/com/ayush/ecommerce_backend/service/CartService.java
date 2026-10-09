package com.ayush.ecommerce_backend.service;

import com.ayush.ecommerce_backend.dto.AddToCartRequest;
import com.ayush.ecommerce_backend.dto.CartItemResponse;
import com.ayush.ecommerce_backend.dto.CartResponse;
import com.ayush.ecommerce_backend.dto.UpdateCartItemRequest;
import com.ayush.ecommerce_backend.entity.Cart;
import com.ayush.ecommerce_backend.entity.CartItem;
import com.ayush.ecommerce_backend.entity.Product;
import com.ayush.ecommerce_backend.entity.Users;
import com.ayush.ecommerce_backend.exception.ForbiddenException;
import com.ayush.ecommerce_backend.exception.InsufficientStockException;
import com.ayush.ecommerce_backend.exception.ResourceNotFoundException;
import com.ayush.ecommerce_backend.repository.CartItemRepository;
import com.ayush.ecommerce_backend.repository.CartRepository;
import com.ayush.ecommerce_backend.repository.ProductRepository;
import com.ayush.ecommerce_backend.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class CartService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;


    // CREATE CART
    @Transactional
    public void createCart(Users user) {

        if (cartRepository.findByUser(user).isPresent()) {
            return;
        }

        Cart cart = new Cart();
        cart.setUser(user);

        cartRepository.save(cart);
    }


    // ADD TO CART
    @Transactional
    public void addToCart(
            String username,
            AddToCartRequest request
    ) {

        Users user = getUser(username);

        Cart cart = getCart(user);

        Product product = productRepository
                .findById(request.getProductId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id "
                                        + request.getProductId()
                        )
                );

        if (product.getStock() == null
                || request.getQuantity() > product.getStock()) {

            throw new InsufficientStockException(
                    "Insufficient stock for product "
                            + product.getName()
            );
        }

        CartItem cartItem =
                cartItemRepository
                        .findByCartAndProduct(cart, product)
                        .orElse(null);

        if (cartItem != null) {

            int newQuantity =
                    cartItem.getQuantity()
                            + request.getQuantity();

            if (newQuantity > product.getStock()) {

                throw new InsufficientStockException(
                        "Insufficient stock for product "
                                + product.getName()
                );
            }

            cartItem.setQuantity(newQuantity);

        } else {

            cartItem = new CartItem();

            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(
                    request.getQuantity()
            );
        }

        cartItemRepository.save(cartItem);
    }


    // GET CART
    @Transactional(readOnly = true)
    public CartResponse getCart(String username) {

        Users user = getUser(username);

        Cart cart = getCart(user);

        List<CartItem> cartItems =
                cartItemRepository.findByCart(cart);

        List<CartItemResponse> responses =
                new ArrayList<>();

        BigDecimal total =
                BigDecimal.ZERO;

        for (CartItem item : cartItems) {

            Product product =
                    item.getProduct();

            BigDecimal subtotal =
                    product.getPrice()
                            .multiply(
                                    BigDecimal.valueOf(
                                            item.getQuantity()
                                    )
                            );

            CartItemResponse response =
                    new CartItemResponse();

            response.setCartItemId(item.getId());

            response.setProductId(
                    product.getId()
            );

            response.setProductName(
                    product.getName()
            );

            response.setPrice(
                    product.getPrice()
            );

            response.setQuantity(
                    item.getQuantity()
            );

            response.setSubtotal(subtotal);

            responses.add(response);

            total = total.add(subtotal);
        }

        CartResponse response =
                new CartResponse();

        response.setCartId(cart.getId());
        response.setItems(responses);
        response.setTotalAmount(total);

        return response;
    }


    // UPDATE CART ITEM
    @Transactional
    public void updateCartItem(
            Integer cartItemId,
            UpdateCartItemRequest request,
            String username
    ) {

        CartItem cartItem =
                getCartItemForUser(
                        cartItemId,
                        username
                );

        Product product =
                cartItem.getProduct();

        if (product.getStock() == null
                || request.getQuantity()
                > product.getStock()) {

            throw new InsufficientStockException(
                    "Insufficient stock of product "
                            + product.getName()
            );
        }

        cartItem.setQuantity(
                request.getQuantity()
        );
    }


    // DELETE CART ITEM
    @Transactional
    public void deleteCartItem(
            Integer cartItemId,
            String username
    ) {

        CartItem cartItem =
                getCartItemForUser(
                        cartItemId,
                        username
                );

        cartItemRepository.delete(cartItem);
    }


    // CLEAR CART
    @Transactional
    public void clearCart(String username) {

        Users user = getUser(username);

        Cart cart = getCart(user);

        List<CartItem> cartItems =
                cartItemRepository.findByCart(cart);

        if (!cartItems.isEmpty()) {
            cartItemRepository.deleteAll(cartItems);
        }
    }

    // GET USER
    private Users getUser(String username) {

        return userRepository.findByEmail(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with username "
                                        + username
                        )
                );
    }


    // GET CART
    private Cart getCart(Users user) {

        return cartRepository.findByUser(user)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cart not found for user "
                                        + user.getEmail()
                        )
                );
    }


    // GET CART ITEM + OWNERSHIP CHECK
    private CartItem getCartItemForUser(
            Integer cartItemId,
            String username
    ) {

        CartItem cartItem =
                cartItemRepository.findById(cartItemId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Cart item not found with id "
                                                + cartItemId
                                )
                        );

        String ownerEmail =
                cartItem.getCart()
                        .getUser()
                        .getEmail();

        if (!ownerEmail.equalsIgnoreCase(username)) {

            throw new ForbiddenException(
                    "You are not authorized to modify this cart item"
            );
        }

        return cartItem;
    }
}