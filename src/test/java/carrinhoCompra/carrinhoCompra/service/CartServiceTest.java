package carrinhoCompra.carrinhoCompra.service;

import org.junit.jupiter.api.Test;

import carrinhoCompra.carrinhoCompra.controller.GestaoItem;
import carrinhoCompra.carrinhoCompra.controller.UserClient;
import carrinhoCompra.carrinhoCompra.dto.CartItemRequestDTO;
import carrinhoCompra.carrinhoCompra.exception.UnauthorizedAccessException;
import carrinhoCompra.carrinhoCompra.exception.UsersNotFoundException;
import carrinhoCompra.carrinhoCompra.model.Cart;
import carrinhoCompra.carrinhoCompra.model.CartItem;
import carrinhoCompra.carrinhoCompra.model.Status;
import carrinhoCompra.carrinhoCompra.repository.CartItemRepository;
import carrinhoCompra.carrinhoCompra.repository.CartRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private GestaoItem gestaoItem;

    @InjectMocks
    private CartService cartService;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetCartByUserIdWhenCartExists() {
        Long userId = 1L;
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setStatus(Status.INICIALIZADO);
        when(cartRepository.findByUserId(userId)).thenReturn(Mono.just(cart));
        Mono<Cart> result = cartService.getCartByUserId(userId);
        assertEquals(cart, result.block());
        verify(cartRepository, times(1)).findByUserId(userId);
    }

    public Mono<Cart> getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId)
                .switchIfEmpty(cartService.createNewCart(userId, "defaultName"));
    }


    @Test
    void testCreateNewCart() {
        Long userId = 1L;
        String authToken = "token";
        Cart newCart = new Cart();
        newCart.setUserId(userId);
        newCart.setStatus(Status.INICIALIZADO);
        when(cartRepository.save(any(Cart.class))).thenReturn(Mono.just(newCart));
        verify(cartRepository, times(0)).save(any(Cart.class));
    }

    @Test
    void testAddItemToCart() {
        Long userId = 1L;
        String authToken = "token";
        CartItemRequestDTO requestDTO = new CartItemRequestDTO();
        requestDTO.setItemId(1L);
        requestDTO.setQuantity(2);
        Cart cart = new Cart();
        cart.setId(1L);
        cart.setStatus(Status.INICIALIZADO);
        CartItem cartItem = new CartItem();
        cartItem.setCartId(cart.getId());
        cartItem.setItemId(requestDTO.getItemId());
        cartItem.setQuantity(requestDTO.getQuantity());
        cartItem.setPrecoUnitario(50.0f);
        cartItem.setPrecoTotal(cartItem.getQuantity() * cartItem.getPrecoUnitario());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(Mono.just(cartItem));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(Flux.just(cartItem));
        when(cartRepository.save(any(Cart.class))).thenReturn(Mono.just(cart));
        verify(cartRepository, times(0)).findByUserIdAndStatusNot(userId, Status.INICIALIZADO);
        verify(cartItemRepository, times(0)).save(any(CartItem.class));
    }

    @Test
    void testUpdateStatusToCart() {
        Long userId = 1L;
        String authToken = "token";
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setStatus(Status.INICIALIZADO);
        when(cartRepository.findByUserId(userId)).thenReturn(Mono.just(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(Mono.just(cart));
        verify(cartRepository, times(0)).save(any(Cart.class));
    }

    @Test
    void testRemoveItemFromCart() {
        Long userId = 1L;
        String authToken = "token";
        Long itemId = 1L;
        Cart cart = new Cart();
        cart.setId(1L);
        CartItem cartItem = new CartItem();
        cartItem.setId(itemId);
        cartItem.setCartId(cart.getId());
        when(cartRepository.findByUserId(userId)).thenReturn(Mono.just(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(Flux.just(cartItem));
        when(cartItemRepository.delete(cartItem)).thenReturn(Mono.empty());
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(Flux.empty());
        when(cartRepository.save(cart)).thenReturn(Mono.just(cart));
        Mono<Cart> result = cartService.removeItemFromCart(userId, authToken, itemId);
        assertEquals(cart, result.block());
    }
}
