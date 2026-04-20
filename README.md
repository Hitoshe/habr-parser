# Habr to Markdown Converter 📝

A powerful and elegant web tool designed to convert [Habr](https://habr.com) articles into Markdown format. Perfect for users who use **Obsidian**, **Notion**, or other PKM (Personal Knowledge Management) tools for offline reading and archiving.

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)

## ✨ Features

- **Deep Parsing:** Extracts full article content, preserving headers, lists, and quotes.
- **Inline Formatting:** Supports bold, italic, and inline code conversion.
- **Smart Image Handling:** Detects lead images and processes lazy-loaded images with absolute URLs.
- **Comments Support:** Fetches comments directly via the official Habr API, bypassing client-side rendering issues.
- **Obsidian Ready:** Automatically generates YAML Frontmatter (title, URL, parse date, tags).
- **Night Edition UI:** A modern, dark-themed web interface with animated starfields for a premium user experience.
- **Stateless & Fast:** No database required. Parse and download instantly.

## 🛠 Tech Stack

- **Backend:** Java 17+, Spring Boot 3.x
- **Parsing:** Jsoup (HTML traversal and manipulation)
- **JSON Processing:** Jackson (for API comment extraction)
- **Frontend:** HTML5, CSS3 (Modern Flexbox/Animations), Vanilla JavaScript
- **Build Tool:** Maven

## 🚀 Getting Started

### Prerequisites
- JDK 17 or higher
- Maven 3.6+

### Installation & Running

1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/habr-parser.git
   cd habr-parser
