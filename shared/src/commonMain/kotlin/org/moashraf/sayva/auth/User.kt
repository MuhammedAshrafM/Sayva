package org.moashraf.sayva.auth

/**
 * Domain-level user representation. Provider-agnostic — this type is deliberately
 * decoupled from any vendor SDK (Firebase's `FirebaseUser`, Supabase's `UserInfo`, etc.).
 *
 * Adapters in `data/firebase/` and `data/supabase/` are responsible for translating
 * their vendor-specific user types into this shape at the boundary. No vendor type
 * ever crosses into ViewModels or the UI layer.
 *
 * Kept intentionally small — only fields the app actually uses. Extend deliberately
 * when a new screen needs more, don't pre-emptively add fields.
 */
data class User(
    /** Stable, opaque identifier assigned by the auth provider. Do not display. */
    val id: String,
    /** May be null for anonymous / guest sessions. */
    val email: String?,
    /** Display name if the user set one; null otherwise. */
    val displayName: String?,
    /** True if the user signed in as a guest without registering. */
    val isAnonymous: Boolean,
    /** True if the auth provider has confirmed ownership of `email`. */
    val isEmailVerified: Boolean,
)
