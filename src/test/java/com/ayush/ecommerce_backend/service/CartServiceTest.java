
package com.ayush.ecommerce_backend.service;

import com.ayush.ecommerce_backend.dto.AddToCartRequest;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private CartService cartService;

    private Users user;
    private Users anotherUser;
    private Cart cart;
    private Product product;

    @BeforeEach
    void setUp() {
        user = new Users();
        user.setId(1);
        user.setName("Test User");
        user.setEmail("user@example.com");

        anotherUser = new Users();
        anotherUser.setId(2);
        anotherUser.setName("Another User");
        anotherUser.setEmail("another@example.com");

        cart = new Cart();
        cart.setId(1);
        cart.setUser(user);

        product = new Product();
        product.setId(1);
        product.setName("Keyboard");
        product.setPrice(new BigDecimal("1500.00"));
        product.setStock(10);
    }

    private void mockUserAndCart() {
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUser(user))
                .thenReturn(Optional.of(cart));
    }

    @Test
    void createCart_shouldCreateCartWhenNoneExists() {
        when(cartRepository.findByUser(user))
                .thenReturn(Optional.empty());

        cartService.createCart(user);

        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void createCart_shouldNotCreateDuplicateCart() {
        when(cartRepository.findByUser(user))
                .thenReturn(Optional.of(cart));

        cartService.createCart(user);

        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void addToCart_shouldAddNewItemSuccessfully() {
        mockUserAndCart();

        AddToCartRequest request = new AddToCartRequest(1, 2);

        when(productRepository.findById(1))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findByCartAndProduct(cart, product))
                .thenReturn(Optional.empty());

        cartService.addToCart("user@example.com", request);

        verify(cartItemRepository).save(argThat(item ->
                item.getCart() == cart
                        && item.getProduct() == product
                        && item.getQuantity() == 2
        ));
    }

    @Test
    void addToCart_shouldIncreaseQuantityForExistingItem() {
        mockUserAndCart();

        CartItem existingItem = new CartItem();
        existingItem.setId(5);
        existingItem.setCart(cart);
        existingItem.setProduct(product);
        existingItem.setQuantity(3);

        when(productRepository.findById(1))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findByCartAndProduct(cart, product))
                .thenReturn(Optional.of(existingItem));

        cartService.addToCart(
                "user@example.com",
                new AddToCartRequest(1, 2)
        );

        assertEquals(5, existingItem.getQuantity());
        verify(cartItemRepository).save(existingItem);
    }

    @Test
    void addToCart_shouldThrowWhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.addToCart(
                        "missing@example.com",
                        new AddToCartRequest(1, 1)
                )
        );

        verifyNoInteractions(productRepository);
    }

    @Test
    void addToCart_shouldThrowWhenCartDoesNotExist() {
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(cartRepository.findByUser(user))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.addToCart(
                        "user@example.com",
                        new AddToCartRequest(1, 1)
                )
        );
    }

    @Test
    void addToCart_shouldThrowWhenProductDoesNotExist() {
        mockUserAndCart();

        when(productRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.addToCart(
                        "user@example.com",
                        new AddToCartRequest(99, 1)
                )
        );

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void addToCart_shouldRejectQuantityGreaterThanStock() {
        mockUserAndCart();

        when(productRepository.findById(1))
                .thenReturn(Optional.of(product));

        assertThrows(
                InsufficientStockException.class,
                () -> cartService.addToCart(
                        "user@example.com",
                        new AddToCartRequest(1, 11)
                )
        );

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void addToCart_shouldRejectWhenCombinedQuantityExceedsStock() {
        mockUserAndCart();

        CartItem existingItem = new CartItem();
        existingItem.setCart(cart);
        existingItem.setProduct(product);
        existingItem.setQuantity(8);

        when(productRepository.findById(1))
                .thenReturn(Optional.of(product));

        when(cartItemRepository.findByCartAndProduct(cart, product))
                .thenReturn(Optional.of(existingItem));

        assertThrows(
                InsufficientStockException.class,
                () -> cartService.addToCart(
                        "user@example.com",
                        new AddToCartRequest(1, 3)
                )
        );

        assertEquals(8, existingItem.getQuantity());
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void getCart_shouldCalculateItemSubtotalAndCartTotal() {
        mockUserAndCart();

        Product secondProduct = new Product();
        secondProduct.setId(2);
        secondProduct.setName("Mouse");
        secondProduct.setPrice(new BigDecimal("500.00"));
        secondProduct.setStock(20);

        CartItem keyboardItem = new CartItem();
        keyboardItem.setId(10);
        keyboardItem.setCart(cart);
        keyboardItem.setProduct(product);
        keyboardItem.setQuantity(2);

        CartItem mouseItem = new CartItem();
        mouseItem.setId(11);
        mouseItem.setCart(cart);
        mouseItem.setProduct(secondProduct);
        mouseItem.setQuantity(3);

        when(cartItemRepository.findByCart(cart))
                .thenReturn(List.of(keyboardItem, mouseItem));

        CartResponse response =
                cartService.getCart("user@example.com");

        assertEquals(1, response.getCartId());
        assertEquals(2, response.getItems().size());

        assertEquals(
                new BigDecimal("3000.00"),
                response.getItems().get(0).getSubtotal()
        );

        assertEquals(
                new BigDecimal("1500.00"),
                response.getItems().get(1).getSubtotal()
        );

        assertEquals(
                new BigDecimal("4500.00"),
                response.getTotalAmount()
        );
    }

    @Test
    void getCart_shouldReturnEmptyCartWithZeroTotal() {
        mockUserAndCart();

        when(cartItemRepository.findByCart(cart))
                .thenReturn(List.of());

        CartResponse response =
                cartService.getCart("user@example.com");

        assertTrue(response.getItems().isEmpty());
        assertEquals(
                BigDecimal.ZERO,
                response.getTotalAmount()
        );
    }

    @Test
    void updateCartItem_shouldUpdateQuantitySuccessfully() {
        CartItem item = new CartItem();
        item.setId(5);
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(2);

        when(cartItemRepository.findById(5))
                .thenReturn(Optional.of(item));

        cartService.updateCartItem(
                5,
                new UpdateCartItemRequest(6),
                "user@example.com"
        );

        assertEquals(6, item.getQuantity());
    }

    @Test
    void updateCartItem_shouldRejectQuantityAboveStock() {
        CartItem item = new CartItem();
        item.setId(5);
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(2);

        when(cartItemRepository.findById(5))
                .thenReturn(Optional.of(item));

        assertThrows(
                InsufficientStockException.class,
                () -> cartService.updateCartItem(
                        5,
                        new UpdateCartItemRequest(11),
                        "user@example.com"
                )
        );

        assertEquals(2, item.getQuantity());
    }

    @Test
    void updateCartItem_shouldRejectAnotherUsersCartItem() {
        Cart anotherCart = new Cart();
        anotherCart.setId(2);
        anotherCart.setUser(anotherUser);

        CartItem item = new CartItem();
        item.setId(5);
        item.setCart(anotherCart);
        item.setProduct(product);
        item.setQuantity(2);

        when(cartItemRepository.findById(5))
                .thenReturn(Optional.of(item));

        assertThrows(
                ForbiddenException.class,
                () -> cartService.updateCartItem(
                        5,
                        new UpdateCartItemRequest(4),
                        "user@example.com"
                )
        );

        assertEquals(2, item.getQuantity());
    }

    @Test
    void updateCartItem_shouldThrowWhenItemDoesNotExist() {
        when(cartItemRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.updateCartItem(
                        99,
                        new UpdateCartItemRequest(2),
                        "user@example.com"
                )
        );
    }

    @Test
    void deleteCartItem_shouldDeleteOwnedItem() {
        CartItem item = new CartItem();
        item.setId(5);
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(2);

        when(cartItemRepository.findById(5))
                .thenReturn(Optional.of(item));

        cartService.deleteCartItem(5, "user@example.com");

        verify(cartItemRepository).delete(item);
    }

    @Test
    void deleteCartItem_shouldRejectAnotherUsersItem() {
        Cart anotherCart = new Cart();
        anotherCart.setId(2);
        anotherCart.setUser(anotherUser);

        CartItem item = new CartItem();
        item.setId(5);
        item.setCart(anotherCart);
        item.setProduct(product);
        item.setQuantity(2);

        when(cartItemRepository.findById(5))
                .thenReturn(Optional.of(item));

        assertThrows(
                ForbiddenException.class,
                () -> cartService.deleteCartItem(
                        5,
                        "user@example.com"
                )
        );

        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    @Test
    void clearCart_shouldDeleteAllItemsWhenCartIsNotEmpty() {
        mockUserAndCart();

        CartItem item = new CartItem();
        item.setId(5);
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(2);

        when(cartItemRepository.findByCart(cart))
                .thenReturn(List.of(item));

        cartService.clearCart("user@example.com");

        verify(cartItemRepository).deleteAll(List.of(item));
    }

    @Test
    void clearCart_shouldNotDeleteWhenCartIsEmpty() {
        mockUserAndCart();

        when(cartItemRepository.findByCart(cart))
                .thenReturn(List.of());

        cartService.clearCart("user@example.com");

        verify(cartItemRepository, never())
                .deleteAll(anyList());
    }

    @Test
    void clearCart_shouldThrowWhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.clearCart("missing@example.com")
        );
    }
}
