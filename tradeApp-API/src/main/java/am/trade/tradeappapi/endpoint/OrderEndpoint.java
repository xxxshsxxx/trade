package am.trade.tradeappapi.endpoint;

import am.trade.tradeappapi.dto.AddOrderDto;
import am.trade.tradeappapi.dto.FindOrderDto;
import am.trade.tradeappapi.dto.ItemMainDto;
import am.trade.tradeappapi.dto.OrderItemDto;
import am.trade.tradeappapi.security.CurrentUser;
import am.trade.tradeappcommon.model.Items;
import am.trade.tradeappcommon.model.Order;
import am.trade.tradeappcommon.model.OrderItem;
import am.trade.tradeappcommon.service.ItemService;
import am.trade.tradeappcommon.service.OrderItemService;
import am.trade.tradeappcommon.service.OrderService;
import am.trade.tradeappcommon.service.PeopleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/rest/orders")
public class OrderEndpoint {

    private final PeopleService peopleService;
    private final OrderService orderService;
    private final ItemService itemService;
    private final OrderItemService orderItemService;

    public OrderEndpoint(PeopleService peopleService, OrderService orderService, ItemService itemService, OrderItemService orderItemService) {
        this.peopleService = peopleService;
        this.orderService = orderService;
        this.itemService = itemService;
        this.orderItemService = orderItemService;
    }

    @PostMapping
    public ResponseEntity saveOrder(@RequestBody AddOrderDto addOrderDto, @AuthenticationPrincipal CurrentUser currentUser) {
        Order order = new Order();
        order.setUser(currentUser.getUser());
        order.setPeople(peopleService.getPeopleByPhone(addOrderDto.getPhoneNumber()));
        for (OrderItemDto orderItemDto : addOrderDto.getOrderItemDtos()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            Items itemById = itemService.getItemById(orderItemDto.getItemId());
            orderItem.setTitle(itemById.getTitle());
            orderItem.setDescription(itemById.getDescription());
            orderItem.setCount(orderItemDto.getCount());
            orderItem.setCategoryName(itemById.getCategory().getName());
            orderItem.setPriceIn(itemById.getPriceIn());
            if ((orderItemDto.getPriceOut() == 0.0)) {
                orderItem.setPriceOut(itemById.getPriceOut());
            } else {
                orderItem.setPriceOut(orderItemDto.getPriceOut());
            }
            double itemCount = itemService.getItemById(orderItemDto.getItemId()).getCount();
            double differenceCount = itemCount - orderItem.getCount();
            if (itemCount >= orderItemDto.getCount()) {
                orderService.addOrder(order);
                Items items = itemService.getItemById(orderItemDto.getItemId());
                items.setCount(differenceCount);
                itemService.saveItem(items);
                orderItemService.saveOrderItem(orderItem);
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
        }
        return ResponseEntity.ok("Order successful saved");
    }

//    -----saveOrder - JSON-----
//{ {
//    "phoneNumber": "093-987456",
//            "orderItemDtos": [
//    {
//        "itemId": "10",
//            "count": "1",
//            "priceOut": "1200"
//    },
//    {
//        "itemId": "2",
//            "count": "2",
//            "priceOut": "0"
//    }]
//}

    @GetMapping
    public List<Order> allOrders() {
        return orderService.findAllOrders();
    }


    @GetMapping("/range/{date}")
    public ResponseEntity searchByDateRange(@PathVariable("date") String date) throws ParseException {
        String[] tmp = date.split(",");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<FindOrderDto> findOrderDtoList = new LinkedList<>();
        List<Order> result = orderService.searchByDateRange(dateFormat.parse(tmp[0]), dateFormat.parse(tmp[1]));

        for (Order order : result) {
            List<OrderItem> byOrderId = orderItemService.findByOrderId(order.getId());
            double orderSum = 0.0;
//            Ապրանքի լիստ
            List<ItemMainDto> itemMainDtoList = new ArrayList<>();
            double orderItemSum = 0.0;
            for (OrderItem orderItem : byOrderId) {
                ItemMainDto itemMainDto = new ItemMainDto();
                itemMainDto.setId(orderItem.getId());
                itemMainDto.setCount(orderItem.getCount());
                itemMainDto.setTitle(orderItem.getTitle());
                itemMainDto.setPriceOut(orderItem.getPriceOut());
                itemMainDtoList.add(itemMainDto);
                orderItemSum += orderItem.getPriceOut() * orderItem.getCount();
            }
//            Ապրանքի լիստ վերջ
            orderSum += orderItemSum;
            FindOrderDto findOrderDto = new FindOrderDto();
            findOrderDto.setDate(order.getDate());
            findOrderDto.setItemMainDtos(itemMainDtoList);
            findOrderDto.setOrderSum(orderSum);
            findOrderDto.setPeople(order.getPeople());
            findOrderDtoList.add(findOrderDto);
        }
        return ResponseEntity.ok(findOrderDtoList);
    }
}