package com.dreweaster.ddd.jester.example.domain.aggregates.user.commands;

import com.dreweaster.ddd.jester.example.domain.util.DomainStyle;
import org.immutables.value.Value;

/**
 */
@Value.Immutable
@DomainStyle
public abstract class AbstractRegisterUser implements UserCommand {

    abstract String username();

    abstract String password();
}
