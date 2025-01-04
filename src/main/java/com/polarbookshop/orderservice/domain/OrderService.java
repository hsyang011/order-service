package com.polarbookshop.orderservice.domain;

import com.polarbookshop.orderservice.book.Book;
import com.polarbookshop.orderservice.book.BookClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service // 이 클래스가 스프링에 관리되는 서비스임을 표시하는 스테레오 타입 애노테이션
public class OrderService {

    private final BookClient bookClient;
    private final OrderRepository orderRepository;

    public OrderService(BookClient bookClient, OrderRepository orderRepository) {
        this.bookClient = bookClient;
        this.orderRepository = orderRepository;
    }

    public Flux<Order> getAllOrders() { // 플럭스틑 여러 개의 주문을 위해 사용된다. (O..N)
        return orderRepository.findAll();
    }

    public Mono<Order> submitOrder(String isbn, int quantity) {
        return bookClient.getBookByIsbn(isbn) // 카탈로그 서비스를 호출해 책의 주문 가능성을 확인한다.
                .map(book -> buildAcceptedOrder(book, quantity)) // 책 주문이 가능하면 접수한다.
                .defaultIfEmpty(buildRejectedOrder(isbn, quantity)) // 책이 카탈로그에 존재하지 않으면 주문을 거부한다.
                .flatMap(orderRepository::save); // 주문을 접수 혹은 거부 상태로 저장한다.
    }

    public static Order buildAcceptedOrder(Book book, int quantity) {
        return Order.of(book.isbn(), book.title() + " - " + book.author(), book.price(), quantity, OrderStatus.ACCCEPTED); // 주문이 접수되면 ISBN, 책의 이름(제목과 저자), 수량, 상태만 지정하면 스프링 데이터가 식별자, 버전, 감사 메타데이터를 추가한다.
    }

    public static Order buildRejectedOrder(String bookIsbn, int quantity) {
        return Order.of(bookIsbn, null, null, quantity, OrderStatus.REJECTED);
    }

}
