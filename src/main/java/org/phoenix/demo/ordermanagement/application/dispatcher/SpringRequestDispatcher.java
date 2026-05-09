package org.phoenix.demo.ordermanagement.application.dispatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.phoenix.demo.ordermanagement.application.abstractions.cqrs.Next;
import org.phoenix.demo.ordermanagement.application.abstractions.cqrs.PipelineBehavior;
import org.phoenix.demo.ordermanagement.application.abstractions.cqrs.Request;
import org.phoenix.demo.ordermanagement.application.abstractions.cqrs.RequestDispatcher;
import org.phoenix.demo.ordermanagement.application.abstractions.cqrs.RequestHandler;
import org.springframework.core.GenericTypeResolver;

public class SpringRequestDispatcher implements RequestDispatcher {

    private final Map<Class<?>, RequestHandler<?, ?>> handlersByRequestType;
    private final List<PipelineBehavior<?, ?>> behaviors;

    public SpringRequestDispatcher(List<RequestHandler<?, ?>> handlers,
                                   List<PipelineBehavior<?, ?>> behaviors) {
        this.handlersByRequestType = indexHandlersByRequestType(handlers);
        this.behaviors = List.copyOf(behaviors);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R, REQ extends Request<R>> R dispatch(REQ request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }

        RequestHandler handler = handlersByRequestType.get(request.getClass());
        if (handler == null) {
            throw new IllegalStateException(
                "No handler registered for request type: " + request.getClass().getName());
        }

        Next<R> chain = () -> (R) handler.handle(request);

        for (int i = behaviors.size() - 1; i >= 0; i--) {
            PipelineBehavior behavior = behaviors.get(i);
            Next<R> currentChain = chain;
            chain = () -> (R) behavior.handle(request, currentChain);
        }

        return chain.proceed();
    }

    private static Map<Class<?>, RequestHandler<?, ?>> indexHandlersByRequestType(
            List<RequestHandler<?, ?>> handlers) {
        Map<Class<?>, RequestHandler<?, ?>> index = new HashMap<>();
        List<String> conflicts = new ArrayList<>();

        for (RequestHandler<?, ?> handler : handlers) {
            Class<?> requestType = resolveRequestType(handler);
            RequestHandler<?, ?> previous = index.put(requestType, handler);
            if (previous != null) {
                conflicts.add(requestType.getName()
                    + " is handled by both "
                    + previous.getClass().getName() + " and "
                    + handler.getClass().getName());
            }
        }

        if (!conflicts.isEmpty()) {
            throw new IllegalStateException(
                "Multiple handlers registered for the same request type: " + conflicts);
        }
        return Map.copyOf(index);
    }

    private static Class<?> resolveRequestType(RequestHandler<?, ?> handler) {
        Class<?>[] typeArgs = GenericTypeResolver.resolveTypeArguments(
            handler.getClass(), RequestHandler.class);
        if (typeArgs == null || typeArgs.length == 0 || typeArgs[0] == null) {
            throw new IllegalStateException(
                "Cannot resolve generic Request type for handler: "
                + handler.getClass().getName()
                + ". Make sure the handler implements CommandHandler or QueryHandler"
                + " with concrete type arguments.");
        }
        return typeArgs[0];
    }
}