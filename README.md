# Jester
#### A simple DDD, CQRS and Eventsourcing library for Java

Jester was dreamed up to be a simple, lightweight library to support building Java applications based on DDD/CQRS/Eventsourcing techniques. There's definitely an emphasis here on _library_ - Jester is not intended to be a bulky _framework_. The objective is to provide a set of simple abstractions that enable developers to overcome the typical, practical challenges commonly faced when building DDD applications.

Jester takes inspiration in places from Lightbend's [Lagom Framework](http://lagomframework.com), especially in the way Aggregate behaviours are specified. However, it differs considerably under the covers, aiming to provide a much simpler operational model (e.g. no need to run an Akka cluster). Lagom is a large, opinionated framework that covers the end-to-end creation of reactive microservices - Jester focuses only on your domain model. In fact, it would be perfectly possible to use Jester within a Lagom application as a drop in replacement for Lagom's persistent entity and persistent read side support.

Jester's design is sympathetic to those wishing to follow the [hexagonal architecture](http://alistair.cockburn.us/Hexagonal+architecture) pattern (aka Ports and Adapters). Jester allows you to keep your domain model isolated from the outer application and infrastructure layers, thus preventing technical concerns leaking into the domain, and making it very simple to test your business invariants in clear, concise ways.

## User Manual

### The basics

Jester is a DDD library containing a set of abstractions to help you build an eventsourced domain model. It is the intention for Jester to be an enabler of event-driven systems. The key concepts you'll encounter are:

* ***Aggregates*** - An aggregate defines a cluster of entities and value objects that form a consistency boundary within which you can enforce business invariants. Aggregates handle commands and emit events in reaction to those commands. In DDD, it's so important to model behaviours, not data. Jester provides a simple behaviour oriented abstraction that helps you to focus on implementing aggregates with a behavioural slant.

* ***Commands*** - A command describes an action to do something, e.g. CreateOrder. They should always be phrased in the correct tense, ensuring it's clear a command is a call to do something, not describe something that has happened. Commands are sent to aggregates to trigger a transition in state/behaviour.

* ***Events*** - An event describes something that has happened, the result of an action having taken place. Events are emitted by aggregates in response to having processed a command. They should always be phrased in past tense, e.g. OrderCreated. It's perfectly feasible for a single command sent to an aggregate to result in many events being emitted.

### Aggregates

An aggregate defines a cluster of entities and value objects that form a consistency boundary within which you can enforce business invariants. In Jester, transactionality is only applied around the conistency boundary of a single aggregate instance - you can't update multiple aggregates in a single transaction. Where changes in one aggregate instance need to result in changes in another, this must be achieved an an eventually consistent, event-driven way.

DDD encourages modelling behaviours, which is opposed to the more traditional way developers tend to model domains (using a data-oriented approach). Jester enforces that aggregates are defined in a behaviour-oriented way using an abstraction inspired greatly by the Persistent Entity concept within the [Lagom Framework](http://lagomframework.com).

To define an aggregate, you need to extend the `Aggregate` abstract class, and specify three type parameters:

*  The base class for commands that the aggregate handles (should be an abstract class extending `DomainComamnd`)
*  The base class for events that the aggregate emits (should be an abstract class extending `DomainEvent`)
*  The class that defines the internal state of the aggregate

We define an aggregate like this:

```java
public class UserAggregate extends Aggregate<UserCommand, UserEvent, UserState> {
}
```

#### Defining behaviour

The next step is to start defining our aggregate's behaviour using the behaviour-oriented DSL.

At any point in time, an instance of an aggregate has a current `Behaviour`. Behaviour is an abstraction that can be seen somewhat like specific state in a state machine. As an aggregate handles commands over time, it can switch between behaviours. A `Behaviour` is composed of three things:

* The current state of the aggregate instance
* Command handlers that define how the behaviour handles commands sent to it
* Event handlers that define how the behaviour translates events into changes in the aggregate's state and, optionally, the shift to a new behaviour (analogous to switching a state machine to a new state).

An aggregate must start off with an initial behaviour. This is  the way an instance of the aggregate should behave before it has processed its first command. This is essentially the aggregate's _pre-creation_ behaviour. To define this initial behaviour, we need to implement the abstract `initialBehaviour()` method from the `Aggregate` base class.

```java
protected Behaviour<UserCommand, UserEvent, UserState> initialBehaviour() {
}
```

Jester enables a simple builder pattern for defining the command and event handlers that make up a `Behaviour`. The method `newBehaviourBuilder(State state)` from the abstract base class is your friend.

A builder needs to be instantiated with an initial _pre-creation_ state. To continue with our example, let's define `UserState` and create a singleton instance to represent the initial empty state:

```java
public class UserState {

    public static UserState EMPTY = of("","","");

    public static UserState of(String username, String password, String salt) {
        return new UserState(username, password, salt);
    }

    private String username;
    
    private String password;
    
    private String salt;

    private UserState(String username, String password, String salt) {
        this.username = username;
        this.password = password;
        this.salt = salt;
    }
}
```

Then, we can instantiate a new `BehaviourBuilder` as part of defining the initial behaviour for the `UserAggregate`:

```java
protected Behaviour<UserCommand, UserEvent, UserState> initialBehaviour() {
   BehaviourBuilder<UserCommand, UserEvent, UserState> behaviourBuilder = newBehaviourBuilder(UserState.EMPTY);
}
```

We're now ready to define our first command handler!

##### Command Handlers

A command handler's responsibility is to handle an incoming command, apply the business logic of the aggregate, and emit one or more events that describe what happened. As an aggregate may behave differently - i.e. can transition over time between different `Behaviour`s - each behaviour can define different command handlers (although it's perfectly ok for different behaviours to share some command handlers).

In our example here, we're going to define the initial behaviour of our `UserAggregate`. The initial behaviour captures the state of the aggregate before the user has been registered to use our product, and, as such, the only command applicable to this behaviour is `RegisterUser`. As a convention, Jester encourages you to drop extensions such as _Command_ and _Event_ from the class names of your commands and events. Make sure your class names capture intent without the need for superfluous extenions.

```java
public class RegisterUser extends UserCommand {

    public static RegisterUser of(String username, String password, String salt) {
        return new RegisterUser(username, password, salt);
    }

    private String username;
    
    private String password;
    
    private String salt;

    public RegisterUser(String username, String password, String salt) {
        this.username = username;
        this.password = password;
        this.salt = salt;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getSalt() {
        return salt;
    }
}
```

In response to this command, we want our aggregate to emit a `UserRegistered` event:

```java
public class UserRegistered extends UserEvent {

    public static UserRegistered of(String username, String password, String salt) {
        return new UserRegistered(username, password, salt);
    }

    private String username;

    private String password;

    private String salt;

    public UserRegistered(String username, String password, String salt) {
        this.username = username;
        this.password = password;
        this.salt = salt;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getSalt() {
        return salt;
    }
}
```

Now that we've defined the command and event, we can add a command handler to the behaviour builder:

```java
behaviourBuilder.setCommandHandler(RegisterUser.class, (cmd, ctx) ->
        ctx.success(UserRegistered.of(
                cmd.getUsername(),
                cmd.getPassword(),
                cmd.getSalt())));
```

You'll notice that we receive a `CommandContext` to which we respond with the event (or events) that's the result of handling the command. If handling a command results in some kind of business logic error, you can respond with `ctx.error(Throwable error)` instead of responding with events.

You'll notice that we don't modify the enclosing aggregate's state within the command handler. This is the responsibility of an event handler.

##### Event handlers

##### Testing

### Command Handling

#### Executing commands

#### Idempotency

#### Optimistic concurrency

##### Deduplication strategies

### The Read-side (CQRS)

#### Aggregate event streams

#### Creating read models

#### Publishing events to messaging middleware

### Future

#### Aggregate state snapshots

#### Process managers

#### Event store implementations

#### Event schema evolutions


