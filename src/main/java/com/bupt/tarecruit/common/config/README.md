# `com.bupt.tarecruit.common.config`

## `DataMountPaths`

Central resolver for on-disk JSON data used by MO/TA features. All paths are built from a single **data root** so deploys can redirect storage without code changes.

### Data root resolution

1. If the environment variable **`mountDataTAMObupter`** is set to a non-empty string, that value is used as the root directory (absolute, normalized).
2. Otherwise the default **`mountDataTAMObupter`** directory relative to the process working directory is used (resolved to an absolute path).

Use `DataMountPaths.fromEnvironment()` to tell whether the root came from the environment or the default.

### API summary

| Method | Resolves to |
| --- | --- |
| `root()` | Data root directory |
| `moDir()` | `<root>/mo` |
| `taDir()` | `<root>/ta` |
| `moRecruitmentCourses()` | `<root>/common/recruitment-courses.json` (shared job board store for MO publish and TA reads) |
| `taAccounts()` | `<root>/ta/tas.json` |
| `taProfiles()` | `<root>/ta/profiles.json` |
| `taApplicationStatus()` | `<root>/ta/application-status.json` |

Constants: `DATA_MOUNT_ENV` is the env var name (`mountDataTAMObupter`); `DEFAULT_DATA_ROOT` is the default relative root path.
