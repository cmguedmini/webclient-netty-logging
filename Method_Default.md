## Analyse de `invocation.getThis()` dans ce contexte

### Réponse courte : **Non, ce n'est pas correct** dans la plupart des cas avec AOP Spring.

---

### Le problème

`invocation.getThis()` retourne le **proxy Spring** (l'objet AOP wrappé), **pas l'instance cible réelle**.

Or, pour invoquer une méthode `default` d'une interface via réflexion, il faut l'instance **cible réelle** (le bean sous-jacent), pas le proxy.

```java
serviceAction.getMethod().invoke(invocation.getThis(), requestBean);
//                                ^^^^^^^^^^^^^^^^^^^
//                    ❌ Retourne le proxy, pas le bean réel
```

---

### Pourquoi c'est problématique

Quand tu appelles `Method.invoke()` sur une méthode `default` d'interface avec un proxy CGLIB/JDK :

| Scénario | Résultat |
|---|---|
| Proxy JDK (`$Proxy`) | `IllegalArgumentException` ou boucle infinie |
| Proxy CGLIB | Comportement imprévisible, souvent boucle infinie |
| Instance réelle | ✅ Fonctionne correctement |

---

### La solution correcte

Pour invoquer une méthode `default` d'interface, il faut utiliser `MethodHandles.Lookup` :

```java
if (isDefaultImplementation(serviceAction.getMethod())) {
    // ✅ Méthode correcte pour invoquer une default method
    Class<?> declaringClass = serviceAction.getMethod().getDeclaringClass();
    
    return MethodHandles.lookup()
        .in(declaringClass)
        .unreflectSpecial(serviceAction.getMethod(), declaringClass)
        .bindTo(invocation.getThis()) // Le proxy est OK ici avec MethodHandles
        .invokeWithArguments(requestBean);
}
```

Ou alternativement, en récupérant la **cible réelle** via le `MethodInvocation` :

```java
if (isDefaultImplementation(serviceAction.getMethod())) {
    // Accéder à la cible réelle (sous le proxy)
    Object target = ((ReflectiveMethodInvocation) invocation).getThis();
    // Non... même problème.
    
    // ✅ Plutôt : laisser l'invocation continuer normalement
    return invocation.proceed();
}
```

---

### Recommandation

La solution la plus propre selon le contexte :

```java
if (isDefaultImplementation(serviceAction.getMethod())) {
    // Option 1 : laisser Spring gérer l'invocation
    return invocation.proceed();
    
    // Option 2 : MethodHandles (Java 7+, fiable pour default methods)
    Class<?> iface = serviceAction.getMethod().getDeclaringClass();
    return MethodHandles.lookup()
        .in(iface)
        .unreflectSpecial(serviceAction.getMethod(), iface)
        .bindTo(invocation.getThis())
        .invokeWithArguments(requestBean);
}
```

> **Note :** `invocation.proceed()` est généralement préféré car il respecte la chaîne d'intercepteurs AOP, contrairement à un appel direct par réflexion.


Tu peux utiliser **`Mockito.mock()` et `Mockito.when()` directement** sans annotation, avec `@BeforeEach` pour l'initialisation :

```java
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MyMethodInterceptorTest {

    // --- Interface avec une default method pour le test ---
    interface MyService {
        default String myDefaultMethod(Object request) {
            return "default-result-" + request;
        }
        String myAbstractMethod(Object request);
    }

    static class MyServiceImpl implements MyService {
        @Override
        public String myAbstractMethod(Object request) {
            return "concrete-result";
        }
    }

    // Déclaration sans annotation
    private MethodInvocation invocation;
    private MyMethodInterceptor interceptor;

    @BeforeEach
    void setUp() {
        // Initialisation manuelle des mocks
        invocation = mock(MethodInvocation.class);
        interceptor = new MyMethodInterceptor();
    }

    @Test
    @DisplayName("default method → exécutée via MethodHandles avec le bon résultat")
    void invoke_shouldInvokeDefaultMethodViaMethodHandles() throws Throwable {
        // GIVEN
        MyServiceImpl targetInstance = new MyServiceImpl();
        Method defaultMethod = MyService.class.getMethod("myDefaultMethod", Object.class);

        when(invocation.getMethod()).thenReturn(defaultMethod);
        when(invocation.getArguments()).thenReturn(new Object[]{"myRequest"});
        when(invocation.getThis()).thenReturn(targetInstance);

        // WHEN
        Object result = interceptor.invoke(invocation);

        // THEN
        assertThat(result).isEqualTo("default-result-myRequest");
        verify(invocation, never()).proceed();
    }

    @Test
    @DisplayName("default method sans argument → requestBean null, pas d'exception")
    void invoke_shouldHandleDefaultMethodWithNoArgs() throws Throwable {
        // GIVEN
        MyServiceImpl targetInstance = new MyServiceImpl();
        Method defaultMethod = MyService.class.getMethod("myDefaultMethod", Object.class);

        when(invocation.getMethod()).thenReturn(defaultMethod);
        when(invocation.getArguments()).thenReturn(new Object[0]);
        when(invocation.getThis()).thenReturn(targetInstance);

        // WHEN
        Object result = interceptor.invoke(invocation);

        // THEN
        assertThat(result).isEqualTo("default-result-null");
    }

    @Test
    @DisplayName("méthode non-default → MethodHandles NON utilisé")
    void invoke_shouldNotUseMethodHandlesForNonDefaultMethod() throws Throwable {
        // GIVEN
        Method abstractMethod = MyService.class.getMethod("myAbstractMethod", Object.class);

        when(invocation.getMethod()).thenReturn(abstractMethod);
        when(invocation.getArguments()).thenReturn(new Object[]{"myRequest"});
        when(invocation.proceed()).thenReturn("proceeded-result");

        // WHEN
        Object result = interceptor.invoke(invocation);

        // THEN
        assertThat(result).isNotEqualTo("default-result-myRequest");
        verify(invocation, never()).getThis();
    }
}
```

---

### Ce qui change

| Avant | Après |
|---|---|
| `@ExtendWith(MockitoExtension.class)` | ❌ Supprimé |
| `@Mock private MethodInvocation invocation` | `private MethodInvocation invocation` (simple champ) |
| Injection automatique par Mockito | `mock(MethodInvocation.class)` dans `@BeforeEach` |

> C'est l'approche la plus légère possible avec JUnit 5 + Mockito, sans aucune extension ni annotation Mockito.
