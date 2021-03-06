package com.dreweaster.ddd.jester.domain;

import com.dreweaster.ddd.jester.domain.AggregateRepository.AggregateRoot.NoHandlerForCommand;
import com.dreweaster.ddd.jester.domain.AggregateRepository.AggregateRoot.NoHandlerForEvent;
import io.vavr.Function2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Either;

public class Behaviour<C extends DomainCommand, E extends DomainEvent, State> {

    private State state;

    private Map<Class<? extends C>, Function2<? extends C, CommandContext<E, State>, Either<Throwable, List<E>>>> commandHandlers = HashMap.empty();

    private Map<Class<? extends E>, Function2<? extends E, Behaviour<C, E, State>, Behaviour<C, E, State>>> eventHandlers = HashMap.empty();

    // TODO: Is there some way this can be avoided?
    private Map<Class, Function2> untypedCommandHandlers = HashMap.empty();

    // TODO: Is there some way this can be avoided?
    private Map<Class, Function2> untypedEventHandlers = HashMap.empty();

    public Behaviour(
            State state,
            Map<Class<? extends C>, Function2<? extends C, CommandContext<E, State>, Either<Throwable, List<E>>>> commandHandlers,
            Map<Class<? extends E>, Function2<? extends E, Behaviour<C, E, State>, Behaviour<C, E, State>>> eventHandlers) {
        this.state = state;
        this.commandHandlers = commandHandlers;
        this.eventHandlers = eventHandlers;

        // TODO: Nasty hack to overlook my inability to work around the generic type system
        this.untypedCommandHandlers = Map.narrow(commandHandlers);
        this.untypedEventHandlers = Map.narrow(eventHandlers);
    }

    public State state() {
        return state;
    }

    @SuppressWarnings("unchecked")
    public final Either<Throwable, List<E>> handleCommand(C command, CommandContext<E, State> commandContext) {
        return untypedCommandHandlers.get(command.getClass()).map(handler ->
                (Either<Throwable, List<E>>) handler.apply(command, commandContext)).getOrElseThrow(
                () -> new NoHandlerForCommand(command));
    }

    @SuppressWarnings("unchecked")
    public final Behaviour<C, E, State> handleEvent(E event) {
        return untypedEventHandlers.get(event.getClass())
                .map(handler -> applyUntypedEventHandler(handler, event))
                .getOrElseThrow(() -> new NoHandlerForEvent(event));
    }

    @SuppressWarnings("unchecked")
    private Behaviour<C, E, State> applyUntypedEventHandler(Function2 eventHandler, E event) {
        return (Behaviour<C, E, State>) eventHandler.apply(event, this);
    }

    /**
     * @return new instance with the given newState
     */
    public Behaviour withState(State newState) {
        return new Behaviour<>(newState, commandHandlers, eventHandlers);
    }

    public BehaviourBuilder<C, E, State> builder() {
        return new BehaviourBuilder<>(state, commandHandlers, eventHandlers);
    }
}