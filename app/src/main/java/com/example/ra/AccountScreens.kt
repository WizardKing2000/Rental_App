package com.example.ra

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun AccountScreens(auth: AccountAuth) {
    var roleName by rememberSaveable { mutableStateOf<String?>(null) }
    var page by rememberSaveable { mutableStateOf("login") }
    var email by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    // Passwords are deliberately excluded from saved instance state.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var validation by remember { mutableStateOf<String?>(null) }
    val role = roleName?.let(AccountRole::valueOf)
    fun changePage(next: String) {
        page = next
        password = ""
        confirmation = ""
        validation = null
        auth.clearMessages()
    }
    BackHandler(enabled = role != null && !auth.busy) {
        if (page != "login") changePage("login") else { roleName = null; auth.clearMessages() }
    }
    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(R.drawable.yaadride_logo), "YaadRide", Modifier.size(64.dp))
                Column {
                    Text("YaadRide", fontSize = 30.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text("Your journey starts here")
                }
            }
            Spacer(Modifier.height(12.dp))
            if (role == null) {
                Text("Welcome to YaadRide", style = MaterialTheme.typography.headlineMedium)
                Text("Choose your account to get started.")
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Rent a car", style = MaterialTheme.typography.titleLarge)
                        Text("Find a ride, request dates, and keep track of your trips.")
                        Button(onClick = { roleName = AccountRole.RENTER.name; changePage("login") },
                            enabled = !auth.busy, modifier = Modifier.fillMaxWidth()) { Text("Continue as renter") }
                    }
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("List your car", style = MaterialTheme.typography.titleLarge)
                        Text("Manage your vehicles, prices, booking requests, and rental earnings.")
                        OutlinedButton(onClick = { roleName = AccountRole.OWNER.name; changePage("login") },
                            enabled = !auth.busy, modifier = Modifier.fillMaxWidth()) { Text("Continue as owner") }
                    }
                }
                auth.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (auth.busy) CircularProgressIndicator()
            } else {
                TextButton(onClick = {
                    roleName = null; changePage("login")
                }, enabled = !auth.busy) { Text("‹ Choose account type") }
                Text(when (page) {
                    "register" -> "Create your ${role.label.lowercase()} account"
                    "reset" -> "Reset your password"
                    else -> "${role.label} login"
                }, style = MaterialTheme.typography.headlineMedium)
                Text(if (role == AccountRole.OWNER) "Your cars, bookings, and earnings in one place."
                    else "Find your next ride across Jamaica.")
                if (page == "register") OutlinedTextField(name, { name = it.take(80); validation = null },
                    label = { Text("Full name") }, singleLine = true, enabled = !auth.busy,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(email, { email = it.take(254); validation = null },
                    label = { Text("Email address") }, singleLine = true, enabled = !auth.busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth())
                if (page != "reset") {
                    OutlinedTextField(password, { password = it.take(129); validation = null },
                        label = { Text("Password") }, singleLine = true, enabled = !auth.busy,
                        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = { TextButton(onClick = { visible = !visible }) { Text(if (visible) "Hide" else "Show") } },
                        modifier = Modifier.fillMaxWidth())
                    if (page == "register") {
                        Text("Use at least ${AccountValidation.MIN_PASSWORD_LENGTH} characters.", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(confirmation, { confirmation = it.take(129); validation = null },
                            label = { Text("Confirm password") }, singleLine = true, enabled = !auth.busy,
                            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth())
                    }
                }
                (validation ?: auth.error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                auth.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                if (auth.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Button(enabled = !auth.busy, modifier = Modifier.fillMaxWidth(), onClick = {
                    auth.clearMessages()
                    validation = when (page) {
                        "register" -> AccountValidation.registration(name, email, password, confirmation)
                        else -> when {
                            !AccountValidation.email(email) -> "Enter a valid email address."
                            page != "reset" && password.isEmpty() -> "Enter your password."
                            else -> null
                        }
                    }
                    if (validation == null) when (page) {
                        "register" -> auth.register(role, name.trim(), email.trim(), password)
                        "reset" -> auth.resetPassword(email.trim())
                        else -> auth.login(role, email.trim(), password)
                    }
                }) { Text(when (page) {
                    "register" -> "Create ${role.label.lowercase()} account"
                    "reset" -> "Send reset email"
                    else -> "Log in as ${role.label.lowercase()}"
                }) }
                TextButton(enabled = !auth.busy, onClick = {
                    changePage(if (page == "login") "register" else "login")
                }) { Text(if (page == "login") "New here? Create an account" else "Already registered? Log in") }
                if (page == "login") TextButton(enabled = !auth.busy,
                    onClick = { changePage("reset") }) { Text("Forgot password?") }
            }
        }
    }
}
