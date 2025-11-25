# Product Brief: Phone Manager

**Date:** 2025-11-25
**Author:** Martin
**Status:** Complete - Ready for PRD Development

---

## Executive Summary

Phone Manager is a private, self-hosted location tracking application for Android devices designed for trusted groups such as families or small teams. The application enables real-time location monitoring, intelligent proximity and geofence alerts, movement history visualization, and automation integration via webhooks. Unlike commercial alternatives, Phone Manager provides complete data ownership, privacy control, and deep customization capabilities including integration with home automation systems like Home Assistant and workflow tools like n8n.

**Key Value Proposition:** A private, fully-controlled location tracking solution that commercial alternatives cannot provide - with advanced features, automation capabilities, and zero dependency on third-party services.

---

## Problem Statement

**Current State Frustrations:**

1. **Privacy Concerns:** Commercial location tracking apps (Google Family Link, Life360, etc.) collect and store user data on third-party servers, creating privacy and data ownership concerns for security-conscious users.

2. **Limited Customization:** Existing solutions offer rigid feature sets with no ability to integrate with personal automation systems or trigger custom actions based on location events.

3. **Subscription Costs:** Many commercial solutions require ongoing subscriptions for advanced features.

4. **Feature Gaps:** Commercial apps often lack specific features like:
   - Webhook integration for home automation
   - Secret/discreet mode for minimal visibility
   - Cross-device proximity alerts
   - Flexible geofencing with custom actions

**Quantified Impact:**
- Users must trust third parties with sensitive location data indefinitely
- No ability to trigger smart home actions (lights, heating, security) based on location
- Limited historical data access and export capabilities

**Why Now:** Growing awareness of data privacy issues, increasing smart home adoption, and desire for self-hosted solutions make this the right time for a private location tracking solution.

---

## Proposed Solution

Phone Manager is an Android application with a self-hosted backend that provides:

**Core Approach:**
- **Self-Hosted Architecture:** User deploys their own backend server, maintaining complete data ownership
- **Group-Based Tracking:** Devices organized into groups (e.g., "family") with mutual visibility
- **Intelligent Alerting:** Proximity alerts (person-to-person) and geofence alerts (person-to-place)
- **Automation Integration:** Webhook system for triggering external actions on location events

**Key Differentiators:**
1. **Complete Privacy:** All data stays on user-controlled infrastructure
2. **Secret Mode:** Minimal UI footprint for discreet operation
3. **Webhook Automation:** Native integration with Home Assistant, n8n, and custom systems
4. **Flexible Alerts:** Both proximity (user-to-user) and geofence (user-to-place) alerting

**Ideal User Experience:**
- Install app on family/team devices
- Register devices to a shared group
- View all group members on a real-time map
- Receive alerts when family members arrive/leave locations
- Automatically trigger home automation (e.g., turn on lights when arriving home)
- Review movement history for any device

---

## Target Users

### Primary User Segment

**Profile:** Privacy-conscious parents and family coordinators

**Demographics:**
- Age: 30-55
- Technical comfort: Moderate to high (can deploy simple server applications)
- Family size: 2-6 members with smartphones

**Current Behavior:**
- May use commercial tracking apps reluctantly due to privacy concerns
- Interested in self-hosting and data ownership
- Often already using smart home technology

**Pain Points:**
- Distrust of commercial data handling
- Want to know when children arrive at school/home
- Need to coordinate family logistics (pickups, meetings)

**Goals:**
- Peace of mind knowing family members' locations
- Automation of routine notifications (arrivals/departures)
- Historical location data for reference

### Secondary User Segment

**Profile:** Small business owners tracking field employees or assets

**Demographics:**
- Small business with 2-10 field workers
- Delivery services, maintenance crews, sales representatives

**Current Behavior:**
- Using consumer apps inappropriately for business purposes
- Managing logistics manually

**Pain Points:**
- Need accountability for employee locations during work hours
- Want to optimize routing and scheduling
- Require visit verification for clients

**Goals:**
- Real-time visibility of team locations
- Historical records of work activities
- Integration with business workflows

---

## Goals and Success Metrics

### Business Objectives

*Note: As a personal project, "business objectives" translate to project success criteria.*

1. **Functional Completeness:** Deliver all MVP features in working condition
2. **Reliability:** Achieve 99% uptime for location tracking during active use
3. **Personal Utility:** Replace need for any commercial tracking application
4. **Learning:** Gain experience with Android development, Kotlin, and backend integration

### User Success Metrics

1. **Location Accuracy:** GPS accuracy within 10-50 meters depending on conditions
2. **Update Frequency:** Configurable from 1-30 minute intervals based on user preference
3. **Alert Latency:** Proximity and geofence alerts trigger within 60 seconds of event
4. **Battery Efficiency:** Less than 10% additional battery drain per day with standard tracking
5. **Data Sync:** All location data synchronized to server within 5 minutes (when connected)

### Key Performance Indicators (KPIs)

| KPI | Target | Measurement |
|-----|--------|-------------|
| Location capture rate | >95% | Successful captures / expected captures |
| Alert delivery rate | >99% | Alerts delivered / alerts triggered |
| App crash rate | <1% | Crash-free sessions |
| Background service uptime | >99% | Time running / time expected |
| API response time | <500ms | Average backend response time |

---

## Strategic Alignment and Financial Impact

### Financial Impact

**Investment:**
- Development time: Personal time investment (hobby project)
- Infrastructure: Minimal - self-hosted on existing home server or low-cost VPS ($5-10/month)
- No licensing costs (open-source stack)

**Value Generated:**
- Elimination of commercial tracking app subscriptions (€5-15/month savings)
- Enhanced smart home integration value
- Complete data ownership (priceless for privacy-conscious users)

**Break-even:** Immediate - no commercial investment required

### Company Objectives Alignment

*As a personal project:*

- **Learning Goal:** Advance Kotlin/Android development skills
- **Privacy Goal:** Achieve self-sovereign location data management
- **Automation Goal:** Integrate location awareness into smart home ecosystem

### Strategic Initiatives

1. Build foundation for potential future open-source release
2. Create reusable components for other Android projects
3. Establish personal backend infrastructure patterns

---

## MVP Scope

### Core Features (Must Have)

**1. Secret Mode**
- Discreet notification with minimal visibility (generic name, no GPS icon)
- Hidden activation mechanism (long-press or tap sequence)
- Suppressed UI feedback (no toasts, minimal logging)

**2. Real-Time Map Display**
- Google Maps integration with Compose
- Display current device location
- Display all group members' locations with markers
- Periodic polling for location updates (10-30 second intervals)

**3. Device Registration & Groups**
- Device registration with deviceId, displayName, groupId
- Group-based visibility (devices in same group see each other)
- API: POST /api/devices/register, GET /api/devices?groupId={id}

**4. Proximity Alerts (User-to-User)**
- Define alerts: ownerDeviceId, targetDeviceId, radiusMeters
- Client-side distance calculation (Haversine/Location.distanceTo)
- Local notification on proximity threshold crossing
- State tracking to prevent duplicate alerts (inside/outside debounce)

**5. Location History**
- Store all location points in Room database
- Sync to server via /api/locations/batch
- Filter by date/time range
- Display as polyline on map
- Basic downsampling for performance

**6. Geofencing with Webhooks**
- Define place alerts: name, lat/lng, radius, transition types
- Use Android Geofencing API (Google Play Services)
- Local notification on enter/exit
- Webhook trigger: POST to configured URL with HMAC signature
- Backend webhook management with secret keys

### Out of Scope for MVP

- iOS application
- Web dashboard
- Advanced analytics (heatmaps, pattern detection)
- Panic/SOS button
- Temporary live sharing links
- Speed alerts
- Battery monitoring and alerts
- Named zones with automatic tagging
- Role-based access control
- Data export (CSV, GPX, KML)
- Multi-profile tracking schedules
- Incident/note tagging

### MVP Success Criteria

1. ✅ All 6 core features functional and tested
2. ✅ Stable background tracking for 24+ hours without crash
3. ✅ Location updates received within configured interval
4. ✅ Proximity and geofence alerts triggering correctly
5. ✅ Webhook integration working with Home Assistant
6. ✅ Location history viewable on map with date filtering
7. ✅ Multiple devices (3+) operating in same group simultaneously

---

## Post-MVP Vision

### Phase 2 Features

1. **Panic/SOS Button**
   - One-tap emergency alert with high-frequency location updates
   - Notification to all trusted devices
   - Optional webhook trigger for security systems

2. **Temporary Live Location Sharing**
   - Generate time-limited URLs for sharing location
   - Web-based viewer (no app required)
   - Configurable expiration (15 min to 24 hours)

3. **Speed and Movement Alerts**
   - Alert when device exceeds configured speed
   - Alert when stationary device starts moving (theft detection)

4. **Battery Monitoring**
   - Track battery levels in backend
   - Alert when battery drops below threshold
   - Alert when device goes offline

5. **Named Zones**
   - Define named places (Home, Work, School)
   - Automatic location tagging in history
   - Zone-based alerts ("Left Home", "Arrived at School")

### Long-term Vision

**1-2 Year Horizon:**

1. **Cross-Platform Support**
   - iOS application
   - Web dashboard for desktop viewing

2. **Advanced Analytics**
   - Heatmaps of frequently visited locations
   - Daily/weekly pattern analysis
   - Time spent at locations statistics

3. **Multi-Profile Tracking Schedules**
   - Work hours: frequent tracking (5 min)
   - Off hours: sparse tracking (30 min)
   - Night: minimal or disabled
   - Automatic profile switching

4. **Enhanced Security**
   - Role-based access (Owner, Member, Read-only)
   - Audit logging
   - Encryption at rest

### Expansion Opportunities

1. **Open Source Release**
   - Community contributions
   - Plugin architecture for custom integrations

2. **Asset Tracking**
   - Dedicated hardware trackers (ESP32 + GPS)
   - Vehicle tracking mode

3. **Integration Ecosystem**
   - Native Home Assistant add-on
   - Tasker integration for Android automation
   - IFTTT/Zapier connectors

---

## Technical Considerations

### Platform Requirements

| Requirement | Specification |
|-------------|---------------|
| **Mobile Platform** | Android (minimum SDK 26 / Android 8.0) |
| **Backend** | Self-hosted server (Docker-compatible) |
| **Maps** | Google Maps SDK (requires API key) |
| **Location Services** | Google Play Services for Geofencing API |
| **Network** | HTTPS for all API communication |
| **Database** | Local: Room (SQLite), Server: PostgreSQL or SQLite |

### Technology Preferences

**Android Client:**
- Language: Kotlin
- UI: Jetpack Compose
- Architecture: MVVM with Clean Architecture principles
- DI: Hilt
- Local Storage: Room, DataStore
- Networking: Ktor Client
- Background: WorkManager + Foreground Service
- Maps: Google Maps SDK for Compose

**Backend:**
- Language: Rust
- Framework: Actix-web or Axum (recommended for async performance)
- Database: PostgreSQL (recommended) or SQLite for simplicity
- Deployment: Docker container
- Benefits: Memory safety, excellent performance, low resource usage ideal for self-hosting

### Architecture Considerations

**Client Architecture:**
```
┌─────────────────────────────────────────────┐
│                 UI Layer                     │
│           (Jetpack Compose)                  │
├─────────────────────────────────────────────┤
│               ViewModel                       │
│         (State Management)                   │
├─────────────────────────────────────────────┤
│              Use Cases                        │
│        (Business Logic)                      │
├─────────────────────────────────────────────┤
│             Repository                        │
│    (Data Abstraction)                        │
├──────────────────┬──────────────────────────┤
│   Local Source   │     Remote Source         │
│  (Room/DataStore)│    (Ktor Client)          │
└──────────────────┴──────────────────────────┘
```

**Server Architecture:**
```
┌─────────────────────────────────────────────┐
│              API Layer                       │
│         (REST Endpoints)                     │
├─────────────────────────────────────────────┤
│            Service Layer                     │
│        (Business Logic)                      │
├─────────────────────────────────────────────┤
│           Repository Layer                   │
│         (Data Access)                        │
├─────────────────────────────────────────────┤
│              Database                        │
│         (PostgreSQL/SQLite)                  │
└─────────────────────────────────────────────┘
```

**Key Integrations:**
- Webhook dispatcher for outbound notifications
- HMAC signing for webhook security
- FCM for push notifications (optional, future)

---

## Constraints and Assumptions

### Constraints

1. **Platform:** Android-only for MVP (no iOS, no web)
2. **Technical Skill:** User must be capable of deploying Docker container or running server application
3. **Google Dependency:** Requires Google Play Services for optimal geofencing
4. **Battery vs. Accuracy:** Android background execution limits require tradeoffs between update frequency and battery life
5. **Network Dependency:** Real-time features require network connectivity; offline mode has limited functionality
6. **Single Developer:** Personal project with limited development time

### Key Assumptions

1. **User Trust:** All users in a group trust each other completely (no consent/privacy controls needed)
2. **Server Availability:** User has access to always-on server infrastructure (home server, VPS, etc.)
3. **Android Dominance:** Target users primarily use Android devices
4. **Technical Capability:** Users can configure backend URL, API keys, and basic settings
5. **Google Maps:** Users accept Google Maps API usage (and associated costs for high usage)
6. **Small Scale:** Maximum 10-20 devices per deployment; not designed for enterprise scale

---

## Risks and Open Questions

### Key Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Android background restrictions kill service | High | Medium | Use proper foreground service, WorkManager fallback, battery optimization exemption |
| Google Maps API costs escalate | Medium | Low | Monitor usage; consider OSM/Mapbox alternative for future |
| Geofencing unreliable in some conditions | Medium | Medium | Combine with manual location checks; user education |
| Server downtime causes data loss | High | Low | Local storage buffer; sync when available |
| Battery drain complaints | Medium | Medium | Configurable update intervals; optimize location provider usage |

### Open Questions

1. ~~**Backend Technology:** What language/framework for the backend?~~ → **Resolved: Rust**
2. **Hosting:** Where will the server be hosted? (Home server, cloud VPS, Raspberry Pi?)
3. **Authentication:** API key per device sufficient, or need proper user authentication?
4. **Notification Delivery:** Use FCM for push notifications or rely on polling?
5. **Historical Data Retention:** How long to keep location history? (30 days, 90 days, unlimited?)

### Areas Needing Further Research

1. **Android 14+ Background Restrictions:** New limitations on foreground services and their impact
2. **Geofencing Accuracy:** Real-world testing of Android Geofencing API reliability
3. **Battery Optimization:** Best practices for location tracking with minimal battery impact
4. **Webhook Security:** Best practices for HMAC signing and webhook delivery guarantees
5. **Map Performance:** Efficient rendering of large polylines (1000+ points) on mobile

---

## Appendices

### A. Research Summary

**Competitive Landscape:**
- **Life360:** Popular but privacy concerns, subscription model, limited customization
- **Google Family Link:** Tied to Google ecosystem, limited features, no automation
- **OwnTracks:** Open-source alternative, but limited UI and no group features
- **Traccar:** Comprehensive but complex, primarily focused on GPS hardware devices

**Key Insights:**
- No existing solution combines privacy, ease of use, and automation integration
- Self-hosted solutions exist but lack polish and modern features
- Growing market for privacy-focused alternatives

### B. Stakeholder Input

**Primary Stakeholder:** Martin (Developer/User)

**Requirements Captured:**
- Complete privacy and data ownership
- Family tracking capability
- Smart home integration (Home Assistant)
- Secret/discreet mode for minimal visibility
- Geofencing with custom actions
- Historical location viewing

### C. References

**Technical Documentation:**
- Android Location APIs: developer.android.com
- Google Maps SDK: developers.google.com/maps
- Android Geofencing: developer.android.com/training/location/geofencing
- WorkManager: developer.android.com/topic/libraries/architecture/workmanager

**Similar Projects:**
- OwnTracks: owntracks.org
- Traccar: traccar.org
- Home Assistant Companion: companion.home-assistant.io

---

_This Product Brief serves as the foundational input for Product Requirements Document (PRD) creation._

_Next Steps: Handoff to Product Manager for PRD development using the `workflow prd` command._
