# Global Toast Usage Guide

The toast notification system is now available globally throughout the entire application.

## Usage in Vue Components

```vue
<script setup>
import { useToast } from '@/composables/useToast'

const { success, error, warning, info } = useToast()

// Show success message
const handleSuccess = () => {
  success('Operation completed successfully!')
}

// Show error message
const handleError = () => {
  error('Something went wrong!')
}

// Show warning message
const handleWarning = () => {
  warning('Please be careful!')
}

// Show info message
const handleInfo = () => {
  info('Here is some information')
}
</script>
```

## Usage in Plain JavaScript (anywhere)

```javascript
// Success
window.toast.success('Item saved successfully!')

// Error
window.toast.error('Failed to delete item')

// Warning
window.toast.warning('This action cannot be undone')

// Info
window.toast.info('New update available')

// Custom duration (default is 3000ms)
window.toast.show('Custom message', 'success', 5000)
```

## Toast Types

- `success` - Green gradient background with checkmark
- `error` - Red gradient background with X icon
- `warning` - Orange gradient background with warning icon
- `info` - Blue gradient background with info icon

## Features

- ✅ Slides in from the right with smooth animation
- ✅ Auto-dismisses after 3 seconds (customizable)
- ✅ Multiple toasts stack vertically
- ✅ Beautiful gradient backgrounds
- ✅ Icon indicators for each type
- ✅ Backdrop blur effect
- ✅ Responsive design

## Replace All alert() Calls

Instead of:
```javascript
alert('Success!')
```

Use:
```javascript
window.toast.success('Success!')
```

This provides a much better user experience with modern UI!
