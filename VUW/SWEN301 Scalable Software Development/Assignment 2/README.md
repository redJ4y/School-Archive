### JSON Library Comparison Table

| Criteria              | Gson                                   | Jackson                                                                                              | json.org                                                |
|-----------------------|----------------------------------------|------------------------------------------------------------------------------------------------------|---------------------------------------------------------|
| **Performance**       | Good but generally slower than Jackson | Fastest among the three based on [stress tests](https://github.com/fabienrenaud/java-json-benchmark) | Slower, not ideal for performance-critical applications |
| **Ease of Use**       | Simple and straightforward API         | More feature-rich, steeper learning curve                                                            | Simple, but lacks some advanced features                |
| **Stability**         | Stable, well-tested                    | Stable, well-tested                                                                                  | Stable, but less feature-rich                           |
| **Dependencies**      | Minimal                                | More dependencies due to modular architecture                                                        | Minimal                                                 |
| **Community Support** | Active, part of Google's ecosystem     | Very active, many contributors on GitHub, extensive StackOverflow presence                           | Less active compared to Gson and Jackson                |
| **License**           | Apache License 2.0                     | Apache License 2.0 or LGPL                                                                           | JSON License (somewhat problematic)                     |
| **Documentation**     | Well-documented                        | Extensive documentation                                                                              | Adequate but less comprehensive                         |

---

### JSON Library Selection

For this assignment, **Gson** was a suitable choice. Given that the assignment's requirements are fairly straightforward, Gson's ease of use allowed for faster development. The community support and stability also make it a safe and dependable choice.

Jackson, while faster, offers many features that are not required for this assignment, making its steeper learning curve unnecessary.

Json.org, while simple, has less community support, an obscure license, less comprehensive documentation, etc., making it less ideal compared to Gson and Jackson for most applications.
