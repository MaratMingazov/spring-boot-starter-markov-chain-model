# Spring Boot Starter — Markov Chain Model

A lightweight and generic Markov Chain Model library packaged as a Spring Boot starter.  
This project provides an easy‑to‑integrate Markov model for sequence prediction, supporting configurable order and automatic backoff.

---

## ✨ Features

- **Generic Markov Model** — works with any token type (`T`)
- **Configurable order (N‑gram style)** — model learns context lengths from `1` to `order`
- **Automatic backoff prediction** — attempts longest context first, then backs off to smaller ones
- **Simple persistence** — save/load transitions to/from JSON
- **Spring Boot autoconfiguration** — ready to be injected as a bean

---

## 📘 What is “order”?

The `order` defines the maximum length of context used for prediction.  
For example:

- `order = 1` → classic Markov chain (predict based on last token)
- `order = 3` → predict based on last 1, 2, or 3 tokens (backoff enabled)

During training, the model generates transitions for all context lengths from `1` to `order`.

---

## 📊 How transitions are stored

The model internally stores transitions as:

```kotlin
MutableMap<List<T>, MutableMap<T, Int>>
```

Example:

```json
{
  "A"     : { "B": 3, "C": 1 },
  "A,B"   : { "C": 2 }
}
```

Each context maps to a histogram of next-token occurrences.

---

## 🔮 How prediction works (backoff strategy)

Given a context list:

1. Try longest context (size = `order`)
2. If no transitions — try `order - 1`
3. Continue until size = 1
4. If still no match → return empty prediction

This allows graceful prediction even when full context is unseen.

---

## 🚀 Usage

### 1. Add dependency

```kotlin
dependencies {
    implementation("com.github.maratmingazov:spring-boot-starter-markov-chain-model:<version>")
}
```

### 2. Inject MarkovModel

```kotlin
@Autowired
lateinit var model: MarkovModel<String>
```

### 3. Train the model

```kotlin
model.train(listOf("A", "B", "C", "A"))
```

### 4. Predict

```kotlin
val prediction = model.predict(listOf("A", "B"))
println(prediction.nextTokens)
```

---

## 💾 Persistence

### Save:

```kotlin
model.saveToFile()
```

### Load:

```kotlin
model.loadFromFile()
```

---

## 📄 License

MIT License.
