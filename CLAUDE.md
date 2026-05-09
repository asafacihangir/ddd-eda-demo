# flight-route-api

## Java Setup

Bu proje `.sdkmanrc` dosyası ile Java sürümünü yönetir (şu an: `java=25.0.2-tem`).

**Maven, Java veya `./mvnw` komutlarını çalıştırmadan önce mutlaka şunu yap:**

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env
```

Bu komut `.sdkmanrc` dosyasındaki Java sürümünü aktif eder. Eğer sürüm yüklü değilse `sdk env install` ile yükle.

Doğru sürümün aktif olduğunu kontrol etmek için: `java -version`
