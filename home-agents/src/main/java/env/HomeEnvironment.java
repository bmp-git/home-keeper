package env;

import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.environment.Environment;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HomeEnvironment extends Environment {
    public static final Literal exampleAction = Literal.parseLiteral("action(some)");

    static Logger logger = Logger.getLogger(HomeEnvironment.class.getName());

    private double perception = 10;

    @Override
    public void init(final String[] args) {
    }

    @Override
    public Collection<Literal> getPercepts(String agName) {
        return Collections.singletonList(
                Literal.parseLiteral(String.format("perception(%s)", perception))
        );
    }

    @Override
    public boolean executeAction(final String ag, final Structure action) {
        if (exampleAction.equals(action)) {
            logger.log(Level.INFO, "Acting..");
        }
        return true;
    }
}
