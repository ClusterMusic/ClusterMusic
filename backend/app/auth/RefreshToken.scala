package auth

case class RefreshToken(token: String, userId: Int, expiresAt: java.time.Instant)
