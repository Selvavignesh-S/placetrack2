package com.placetrack.demo;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * BeanFactory vs ApplicationContext. Run this file's main method on its own
 * (IDE: Run 'BeanFactoryDemo.main()'); it does not start the web app.
 *
 * BeanFactory creates a bean only when you ask for it (lazy).
 * ApplicationContext creates singleton beans at startup (eager), before anyone asks.
 */
public class BeanFactoryDemo {

    /** A tiny bean that prints when it is created and when it is used. */
    public static class Greeter {
        public Greeter() {
            System.out.println("      -> Greeter constructor runs (bean created)");
        }

        public String greet() {
            return "Hello from a Spring bean";
        }
    }

    /** A second bean that needs a Greeter: the container injects it through the constructor. */
    public static class WelcomeService {
        private final Greeter greeter;

        public WelcomeService(Greeter greeter) {
            this.greeter = greeter;
        }

        public String welcome() {
            return greeter.greet() + ", injected into WelcomeService";
        }
    }

    public static void main(String[] args) {
        System.out.println("===== Part 1: BeanFactory (lazy) =====");
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.registerBeanDefinition("greeter", new RootBeanDefinition(Greeter.class));
        System.out.println("1. Bean definition registered. Nothing created yet.");
        System.out.println("2. Calling factory.getBean(\"greeter\")...");
        Greeter greeter = factory.getBean("greeter", Greeter.class);
        System.out.println("3. " + greeter.greet());

        System.out.println();
        System.out.println("===== Part 2: ApplicationContext (eager + dependency injection) =====");
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(Greeter.class);
        context.registerBean(WelcomeService.class);
        System.out.println("1. Beans registered. Calling context.refresh()...");
        context.refresh();
        System.out.println("2. refresh() finished: the Greeter was already created above, before any getBean call.");
        WelcomeService welcomeService = context.getBean(WelcomeService.class);
        System.out.println("3. " + welcomeService.welcome());
        context.close();
    }
}
