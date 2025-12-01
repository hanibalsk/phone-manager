# UI Screens Specification

## Phone Manager Android - User Interface Design

**Version:** 1.0.0
**Status:** Design Specification
**Last Updated:** 2025-12-01

---

## 1. Overview

This document specifies the Jetpack Compose UI screens for user management, group management, device management, and settings control features. All designs follow Material Design 3 guidelines.

---

## 2. Navigation Structure

### 2.1 Navigation Graph

```kotlin
// navigation/AppNavGraph.kt
@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = if (isLoggedIn) "home" else "auth"
) {
    NavHost(navController, startDestination) {
        // Auth flow
        navigation(startDestination = "login", route = "auth") {
            composable("login") { LoginScreen(navController) }
            composable("register") { RegisterScreen(navController) }
            composable("forgot_password") { ForgotPasswordScreen(navController) }
            composable("verify_email/{email}") { VerifyEmailScreen(navController, it) }
        }

        // Main app
        navigation(startDestination = "map", route = "home") {
            composable("map") { MapScreen(navController) }
            composable("devices") { DevicesScreen(navController) }
            composable("groups") { GroupListScreen(navController) }
            composable("profile") { ProfileScreen(navController) }
            composable("settings") { SettingsScreen(navController) }
        }

        // Group screens
        composable("group/{groupId}") { GroupDetailScreen(navController, it) }
        composable("group/{groupId}/members") { GroupMembersScreen(navController, it) }
        composable("group/{groupId}/devices") { GroupDevicesScreen(navController, it) }
        composable("group/{groupId}/invites") { GroupInvitesScreen(navController, it) }
        composable("group/{groupId}/unlock_requests") { UnlockRequestsScreen(navController, it) }

        // Device screens
        composable("device/{deviceId}") { DeviceDetailScreen(navController, it) }
        composable("device/{deviceId}/settings") { DeviceSettingsScreen(navController, it) }
        composable("device/{deviceId}/locks") { AdminLockManagementScreen(navController, it) }

        // Invitations
        composable("invitations") { PendingInvitationsScreen(navController) }
        composable("join/{inviteCode}") { JoinGroupScreen(navController, it) }

        // My unlock requests
        composable("my_unlock_requests") { MyUnlockRequestsScreen(navController) }
    }
}
```

### 2.2 Bottom Navigation

```kotlin
// navigation/BottomNavigation.kt
enum class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    MAP("map", Icons.Default.Map, "Map"),
    DEVICES("devices", Icons.Default.Devices, "Devices"),
    GROUPS("groups", Icons.Default.Group, "Groups"),
    PROFILE("profile", Icons.Default.Person, "Profile")
}

@Composable
fun AppBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    pendingInvitationCount: Int = 0
) {
    NavigationBar {
        BottomNavItem.values().forEach { item ->
            NavigationBarItem(
                icon = {
                    if (item == BottomNavItem.GROUPS && pendingInvitationCount > 0) {
                        BadgedBox(badge = { Badge { Text("$pendingInvitationCount") } }) {
                            Icon(item.icon, contentDescription = item.label)
                        }
                    } else {
                        Icon(item.icon, contentDescription = item.label)
                    }
                },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) }
            )
        }
    }
}
```

---

## 3. Authentication Screens

### 3.1 Login Screen

```kotlin
// presentation/ui/auth/LoginScreen.kt
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.Success -> navController.navigate("home") {
                    popUpTo("auth") { inclusive = true }
                }
                is LoginEvent.Error -> { /* Show snackbar */ }
            }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // Logo
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = "Phone Manager",
                modifier = Modifier.size(120.dp)
            )

            Spacer(Modifier.height(32.dp))

            Text(
                "Welcome Back",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Sign in to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            // Email field
            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                isError = uiState.emailError != null,
                supportingText = uiState.emailError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // Password field
            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Password") },
                visualTransformation = if (uiState.showPassword)
                    VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(onClick = viewModel::togglePasswordVisibility) {
                        Icon(
                            if (uiState.showPassword) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = "Toggle password"
                        )
                    }
                },
                singleLine = true,
                isError = uiState.passwordError != null,
                supportingText = uiState.passwordError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // Forgot password
            TextButton(
                onClick = { navController.navigate("forgot_password") },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Forgot Password?")
            }

            Spacer(Modifier.height(24.dp))

            // Login button
            Button(
                onClick = viewModel::login,
                enabled = !uiState.isLoading && uiState.isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Sign In")
                }
            }

            Spacer(Modifier.height(24.dp))

            // Divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(Modifier.weight(1f))
                Text(
                    "OR",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // Social login buttons
            OutlinedButton(
                onClick = viewModel::loginWithGoogle,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Continue with Google")
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = viewModel::loginWithApple,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_apple),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Continue with Apple")
            }

            Spacer(Modifier.height(32.dp))

            // Register link
            Row {
                Text("Don't have an account?")
                TextButton(onClick = { navController.navigate("register") }) {
                    Text("Sign Up")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Continue without account
            TextButton(onClick = { navController.navigate("home") }) {
                Text("Continue without account")
            }
        }
    }
}
```

### 3.2 Register Screen

```kotlin
// presentation/ui/auth/RegisterScreen.kt
@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Display name
            OutlinedTextField(
                value = uiState.displayName,
                onValueChange = viewModel::onDisplayNameChange,
                label = { Text("Display Name") },
                singleLine = true,
                isError = uiState.displayNameError != null,
                supportingText = uiState.displayNameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // Email
            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                isError = uiState.emailError != null,
                supportingText = uiState.emailError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // Password
            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                isError = uiState.passwordError != null,
                supportingText = uiState.passwordError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            // Password strength indicator
            if (uiState.password.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                PasswordStrengthIndicator(strength = uiState.passwordStrength)
            }

            Spacer(Modifier.height(16.dp))

            // Confirm password
            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                isError = uiState.confirmPasswordError != null,
                supportingText = uiState.confirmPasswordError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            // Terms checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.acceptedTerms,
                    onCheckedChange = viewModel::onAcceptTermsChange
                )
                Text(
                    buildAnnotatedString {
                        append("I agree to the ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("Terms of Service")
                        }
                        append(" and ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("Privacy Policy")
                        }
                    },
                    modifier = Modifier.clickable { /* Open terms */ }
                )
            }

            Spacer(Modifier.height(24.dp))

            // Register button
            Button(
                onClick = viewModel::register,
                enabled = !uiState.isLoading && uiState.isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Create Account")
                }
            }
        }
    }
}

@Composable
fun PasswordStrengthIndicator(strength: PasswordStrength) {
    val color = when (strength) {
        PasswordStrength.WEAK -> Color.Red
        PasswordStrength.FAIR -> Color.Yellow
        PasswordStrength.GOOD -> Color.Green
        PasswordStrength.STRONG -> Color.Green
    }

    Column {
        LinearProgressIndicator(
            progress = { strength.ordinal / 3f },
            color = color,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            strength.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
```

---

## 4. Group Screens

### 4.1 Group List Screen

```kotlin
// presentation/ui/group/GroupListScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    navController: NavController,
    viewModel: GroupListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Groups") },
                actions = {
                    // Pending invitations badge
                    if (uiState.pendingInvitationCount > 0) {
                        IconButton(onClick = { navController.navigate("invitations") }) {
                            BadgedBox(
                                badge = { Badge { Text("${uiState.pendingInvitationCount}") } }
                            ) {
                                Icon(Icons.Default.Mail, "Invitations")
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* Show menu */ },
                icon = { Icon(Icons.Default.Add, "Add") },
                text = { Text("New Group") }
            )
        }
    ) { padding ->
        if (uiState.isLoading && uiState.groups.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.groups.isEmpty()) {
            EmptyGroupsState(
                onCreateGroup = { showCreateDialog = true },
                onJoinGroup = { showJoinDialog = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Groups I own
                if (uiState.ownedGroups.isNotEmpty()) {
                    item {
                        Text(
                            "My Groups",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(uiState.ownedGroups) { group ->
                        GroupCard(
                            group = group,
                            onClick = { navController.navigate("group/${group.id}") }
                        )
                    }
                }

                // Groups I admin
                if (uiState.adminGroups.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Administering",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(uiState.adminGroups) { group ->
                        GroupCard(
                            group = group,
                            onClick = { navController.navigate("group/${group.id}") }
                        )
                    }
                }

                // Groups I'm a member of
                if (uiState.memberGroups.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Member of",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(uiState.memberGroups) { group ->
                        GroupCard(
                            group = group,
                            onClick = { navController.navigate("group/${group.id}") }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, description ->
                viewModel.createGroup(name, description)
                showCreateDialog = false
            }
        )
    }

    if (showJoinDialog) {
        JoinGroupDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { code ->
                viewModel.joinWithCode(code)
                showJoinDialog = false
            }
        )
    }
}

@Composable
fun GroupCard(
    group: Group,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Group avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    group.name.take(2).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    group.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${group.memberCount} members • ${group.deviceCount} devices",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Role badge
            group.myRole?.let { role ->
                AssistChip(
                    onClick = {},
                    label = { Text(role.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = when (role) {
                            GroupRole.OWNER -> MaterialTheme.colorScheme.primaryContainer
                            GroupRole.ADMIN -> MaterialTheme.colorScheme.secondaryContainer
                            GroupRole.MEMBER -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                )
            }
        }
    }
}
```

### 4.2 Group Detail Screen

```kotlin
// presentation/ui/group/GroupDetailScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
    viewModel: GroupDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showInviteSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.group?.name ?: "Group") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState.canManageMembers) {
                        IconButton(onClick = { /* Show menu */ }) {
                            Icon(Icons.Default.MoreVert, "More")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group info card
            item {
                GroupInfoCard(
                    group = uiState.group,
                    onEdit = { /* Navigate to edit */ }
                )
            }

            // Quick actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.canInviteMembers) {
                        OutlinedButton(
                            onClick = { showInviteSheet = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PersonAdd, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Invite")
                        }
                    }

                    OutlinedButton(
                        onClick = { navController.navigate("group/${uiState.group?.id}/devices") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Devices, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Devices")
                    }
                }
            }

            // Members section
            item {
                SectionHeader(
                    title = "Members",
                    count = uiState.members.size,
                    onSeeAll = { navController.navigate("group/${uiState.group?.id}/members") }
                )
            }

            items(uiState.members.take(5)) { member ->
                MemberListItem(
                    member = member,
                    canManage = uiState.canManageMembers,
                    onClick = { /* Show member options */ }
                )
            }

            // Pending unlock requests (admin only)
            if (uiState.canManageMembers) {
                item {
                    SectionHeader(
                        title = "Unlock Requests",
                        onSeeAll = { navController.navigate("group/${uiState.group?.id}/unlock_requests") }
                    )
                }
            }

            // Leave/Delete actions
            item {
                Spacer(Modifier.height(16.dp))

                if (uiState.canLeaveGroup) {
                    OutlinedButton(
                        onClick = { viewModel.leaveGroup() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Leave Group")
                    }
                }

                if (uiState.canDeleteGroup) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.deleteGroup() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete Group")
                    }
                }
            }
        }
    }

    if (showInviteSheet) {
        CreateInviteBottomSheet(
            onDismiss = { showInviteSheet = false },
            onCreate = { type, maxUses, expiresInHours ->
                viewModel.createInvite(type, maxUses, expiresInHours)
            },
            createdInvite = uiState.createdInvite
        )
    }
}

@Composable
fun MemberListItem(
    member: GroupMembership,
    canManage: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(member.userDisplayName) },
        supportingContent = {
            Text("${member.deviceCount} devices • Joined ${member.joinedAt.formatRelative()}")
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    member.userDisplayName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoleBadge(role = member.role)
                if (canManage && member.role != GroupRole.OWNER) {
                    IconButton(onClick = onClick) {
                        Icon(Icons.Default.MoreVert, "Options")
                    }
                }
            }
        }
    )
}

@Composable
fun RoleBadge(role: GroupRole) {
    val (color, text) = when (role) {
        GroupRole.OWNER -> MaterialTheme.colorScheme.primary to "Owner"
        GroupRole.ADMIN -> MaterialTheme.colorScheme.secondary to "Admin"
        GroupRole.MEMBER -> MaterialTheme.colorScheme.outline to "Member"
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
```

---

## 5. Device Screens

### 5.1 Devices Screen

```kotlin
// presentation/ui/device/DevicesScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    navController: NavController,
    viewModel: MyDevicesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Devices") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Current device
            uiState.currentDevice?.let { device ->
                item {
                    Text(
                        "This Device",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    CurrentDeviceCard(
                        device = device,
                        onClick = { navController.navigate("device/${device.id}") }
                    )
                }
            }

            // Other devices
            if (uiState.otherDevices.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Other Devices",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(uiState.otherDevices) { device ->
                    DeviceCard(
                        device = device,
                        onClick = { navController.navigate("device/${device.id}") }
                    )
                }
            }
        }
    }
}

@Composable
fun CurrentDeviceCard(
    device: Device,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PhoneAndroid,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    device.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "${device.deviceInfo?.manufacturer} ${device.deviceInfo?.model}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            Surface(
                color = Color.Green.copy(alpha = 0.2f),
                shape = CircleShape
            ) {
                Text(
                    "Active",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Green
                )
            }
        }
    }
}

@Composable
fun DeviceCard(
    device: Device,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (device.platform == DevicePlatform.ANDROID)
                    Icons.Default.PhoneAndroid else Icons.Default.PhoneIphone,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    device.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    device.deviceInfo?.let { "${it.manufacturer} ${it.model}" }
                        ?: device.platform.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                OnlineIndicator(isOnline = device.isOnline)
                device.lastSeenAt?.let { lastSeen ->
                    Text(
                        lastSeen.formatRelative(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun OnlineIndicator(isOnline: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    if (isOnline) Color.Green else Color.Gray,
                    CircleShape
                )
        )
        Spacer(Modifier.width(4.dp))
        Text(
            if (isOnline) "Online" else "Offline",
            style = MaterialTheme.typography.labelSmall,
            color = if (isOnline) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

### 5.2 Device Settings Screen

```kotlin
// presentation/ui/device/DeviceSettingsScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSettingsScreen(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
    viewModel: DeviceSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showUnlockRequestDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.syncSettings() }) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Sync, "Sync")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Locked settings warning
                if (uiState.hasLockedSettings) {
                    item {
                        LockedSettingsWarning(
                            lockedCount = uiState.settingsState?.lockedCount ?: 0
                        )
                    }
                }

                // Settings by category
                uiState.settingsByCategory.forEach { (category, settings) ->
                    item {
                        Text(
                            category.displayName(),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    items(settings) { setting ->
                        SettingItem(
                            setting = setting,
                            isUpdating = uiState.updatingKey == setting.key,
                            onValueChange = { value ->
                                viewModel.updateSetting(setting.key, value)
                            },
                            onRequestUnlock = {
                                showUnlockRequestDialog = setting.key
                            }
                        )
                    }
                }
            }
        }
    }

    showUnlockRequestDialog?.let { settingKey ->
        UnlockRequestDialog(
            settingKey = settingKey,
            settingName = uiState.settingsState?.getSetting(settingKey)
                ?.definition?.displayName ?: settingKey,
            onDismiss = { showUnlockRequestDialog = null },
            onSubmit = { reason ->
                viewModel.requestUnlock(settingKey, reason)
                showUnlockRequestDialog = null
            }
        )
    }
}

@Composable
fun SettingItem(
    setting: DeviceSetting,
    isUpdating: Boolean,
    onValueChange: (SettingValue) -> Unit,
    onRequestUnlock: () -> Unit
) {
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(setting.definition?.displayName ?: setting.key)
                if (setting.isLocked) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        supportingContent = {
            Column {
                setting.definition?.description?.let { desc ->
                    Text(desc)
                }
                if (setting.isLocked) {
                    setting.lockInfo?.reason?.let { reason ->
                        Text(
                            "Locked: $reason",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        },
        trailingContent = {
            if (isUpdating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else if (setting.isLocked) {
                TextButton(onClick = onRequestUnlock) {
                    Text("Request Unlock")
                }
            } else {
                when (setting.value) {
                    is SettingValue.BooleanValue -> {
                        Switch(
                            checked = setting.value.asBoolean(),
                            onCheckedChange = { onValueChange(SettingValue.BooleanValue(it)) }
                        )
                    }
                    is SettingValue.IntValue -> {
                        Text(setting.value.asInt().toString())
                    }
                    is SettingValue.StringValue -> {
                        Text(
                            setting.value.asString(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    else -> {}
                }
            }
        }
    )
}

@Composable
fun LockedSettingsWarning(lockedCount: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "$lockedCount settings locked by admin",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    "Contact your group administrator to request changes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}
```

---

## 6. Profile Screen

```kotlin
// presentation/ui/profile/ProfileScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User info card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                uiState.user?.displayName?.take(2)?.uppercase() ?: "?",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            uiState.user?.displayName ?: "Guest",
                            style = MaterialTheme.typography.titleLarge
                        )

                        uiState.user?.email?.let { email ->
                            Text(
                                email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (uiState.user == null) {
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { navController.navigate("auth") }) {
                                Text("Sign In")
                            }
                        }
                    }
                }
            }

            // Stats
            if (uiState.user != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            label = "Devices",
                            value = "${uiState.deviceCount}",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Groups",
                            value = "${uiState.groupCount}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Menu items
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        if (uiState.user != null) {
                            ProfileMenuItem(
                                icon = Icons.Default.Edit,
                                title = "Edit Profile",
                                onClick = { /* Navigate */ }
                            )
                            HorizontalDivider()
                            ProfileMenuItem(
                                icon = Icons.Default.Security,
                                title = "Security",
                                onClick = { /* Navigate */ }
                            )
                            HorizontalDivider()
                        }
                        ProfileMenuItem(
                            icon = Icons.Default.Help,
                            title = "Help & Support",
                            onClick = { /* Navigate */ }
                        )
                        HorizontalDivider()
                        ProfileMenuItem(
                            icon = Icons.Default.Info,
                            title = "About",
                            onClick = { /* Navigate */ }
                        )
                    }
                }
            }

            // Logout
            if (uiState.user != null) {
                item {
                    OutlinedButton(
                        onClick = viewModel::logout,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = {
            Icon(icon, contentDescription = null)
        },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
```

---

## 7. Dialogs & Bottom Sheets

### 7.1 Create Group Dialog

```kotlin
// presentation/ui/dialog/CreateGroupDialog.kt
@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Group") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, description.ifBlank { null }) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

### 7.2 Join Group Dialog

```kotlin
// presentation/ui/dialog/JoinGroupDialog.kt
@Composable
fun JoinGroupDialog(
    onDismiss: () -> Unit,
    onJoin: (code: String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Group") },
        text = {
            Column {
                Text(
                    "Enter the invite code you received",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().filter { c -> c.isLetterOrDigit() } },
                    label = { Text("Invite Code") },
                    placeholder = { Text("XXXX-XXXX") },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        letterSpacing = 2.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onJoin(code) },
                enabled = code.length >= 6
            ) {
                Text("Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

### 7.3 Unlock Request Dialog

```kotlin
// presentation/ui/dialog/UnlockRequestDialog.kt
@Composable
fun UnlockRequestDialog(
    settingKey: String,
    settingName: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request Unlock") },
        text = {
            Column {
                Text(
                    "Request your administrator to unlock \"$settingName\"",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    placeholder = { Text("Why do you need this setting unlocked?") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(reason) },
                enabled = reason.isNotBlank()
            ) {
                Text("Send Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

### 7.4 Create Invite Bottom Sheet

```kotlin
// presentation/ui/bottomsheet/CreateInviteBottomSheet.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInviteBottomSheet(
    onDismiss: () -> Unit,
    onCreate: (type: InviteType, maxUses: Int?, expiresInHours: Int?) -> Unit,
    createdInvite: GroupInvite?
) {
    var selectedType by remember { mutableStateOf(InviteType.SINGLE_USE) }
    var maxUses by remember { mutableStateOf("") }
    var expiresInHours by remember { mutableStateOf("72") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            if (createdInvite != null) {
                // Show created invite
                Text(
                    "Invite Created!",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(24.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            InviteShareUtils.formatCode(createdInvite.inviteCode),
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Copy to clipboard
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Copy")
                    }

                    Button(
                        onClick = {
                            // Share
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Share")
                    }
                }
            } else {
                // Create invite form
                Text(
                    "Create Invite",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(24.dp))

                // Invite type selection
                Text(
                    "Invite Type",
                    style = MaterialTheme.typography.labelLarge
                )

                Spacer(Modifier.height(8.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    InviteType.values().forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = InviteType.values().size
                            )
                        ) {
                            Text(type.name.replace("_", " "))
                        }
                    }
                }

                if (selectedType == InviteType.MULTI_USE) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = maxUses,
                        onValueChange = { maxUses = it.filter { c -> c.isDigit() } },
                        label = { Text("Max Uses") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = expiresInHours,
                    onValueChange = { expiresInHours = it.filter { c -> c.isDigit() } },
                    label = { Text("Expires In (hours)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        onCreate(
                            selectedType,
                            maxUses.toIntOrNull(),
                            expiresInHours.toIntOrNull()
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Invite")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
```

---

## 8. Theme & Design System

### 8.1 Color Scheme

```kotlin
// presentation/theme/Color.kt
val md_theme_light_primary = Color(0xFF006D3B)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFF99F6B9)
val md_theme_light_onPrimaryContainer = Color(0xFF00210E)
val md_theme_light_secondary = Color(0xFF4F6354)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFD1E8D5)
val md_theme_light_onSecondaryContainer = Color(0xFF0C1F13)
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_background = Color(0xFFFBFDF8)
val md_theme_light_surface = Color(0xFFFBFDF8)

val md_theme_dark_primary = Color(0xFF7DD99E)
val md_theme_dark_onPrimary = Color(0xFF00391C)
val md_theme_dark_primaryContainer = Color(0xFF00522B)
val md_theme_dark_onPrimaryContainer = Color(0xFF99F6B9)
val md_theme_dark_secondary = Color(0xFFB5CCBA)
val md_theme_dark_onSecondary = Color(0xFF213527)
val md_theme_dark_secondaryContainer = Color(0xFF374B3D)
val md_theme_dark_onSecondaryContainer = Color(0xFFD1E8D5)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_background = Color(0xFF191C1A)
val md_theme_dark_surface = Color(0xFF191C1A)
```

### 8.2 Typography

```kotlin
// presentation/theme/Type.kt
val PhoneManagerTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)
```

---

## 9. Utility Extensions

```kotlin
// util/Extensions.kt
fun Instant.formatRelative(): String {
    val now = Instant.now()
    val duration = Duration.between(this, now)

    return when {
        duration.toMinutes() < 1 -> "Just now"
        duration.toMinutes() < 60 -> "${duration.toMinutes()}m ago"
        duration.toHours() < 24 -> "${duration.toHours()}h ago"
        duration.toDays() < 7 -> "${duration.toDays()}d ago"
        else -> {
            val formatter = DateTimeFormatter.ofPattern("MMM d")
                .withZone(ZoneId.systemDefault())
            formatter.format(this)
        }
    }
}

fun SettingCategory.displayName(): String = when (this) {
    SettingCategory.TRACKING -> "Tracking"
    SettingCategory.PRIVACY -> "Privacy"
    SettingCategory.NOTIFICATIONS -> "Notifications"
    SettingCategory.BATTERY -> "Battery"
    SettingCategory.SYNC -> "Sync"
    SettingCategory.DISPLAY -> "Display"
    SettingCategory.SECURITY -> "Security"
}
```

---

## 10. Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-12-01 | Initial specification |
