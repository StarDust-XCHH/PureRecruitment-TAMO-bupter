# MO Model Classes (`com.bupt.tarecruit.mo.model`)

Data transfer objects and request/response models for the MO module.

## Classes

### `MoRegisterRequest`
Encapsulates MO registration form data with validation constraints.

**Fields:**
- `moId` - MO identifier (auto-formatted as "MO-XXXXX")
- `name` - Real name
- `username` - Login username
- `email` - Email address
- `phone` - Phone number
- `password` - Password (min 6 characters)
- `confirmPassword` - Password confirmation

**Usage:**