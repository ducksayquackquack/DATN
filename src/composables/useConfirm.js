import { ref } from 'vue'

const confirmState = ref({
  visible: false,
  mode: 'confirm',
  title: '',
  message: '',
  confirmText: 'Yes',
  cancelText: 'No',
  inputValue: '',
  inputPlaceholder: ''
})

let resolver = null

const closeWith = (value) => {
  if (resolver) {
    resolver(value)
    resolver = null
  }

  confirmState.value = {
    ...confirmState.value,
    visible: false
  }
}

const openDialog = (mode, message, options = {}) => {
  // If another dialog is already open, resolve it with a safe default.
  if (resolver) {
    if (confirmState.value.mode === 'prompt') {
      resolver(null)
    } else if (confirmState.value.mode === 'confirm') {
      resolver(false)
    } else {
      resolver(undefined)
    }
    resolver = null
  }

  const defaultTitle = mode === 'prompt' ? 'Input Required' : 'Please Confirm'
  const confirmText = mode === 'confirm' ? 'Yes' : 'OK'

  confirmState.value = {
    visible: true,
    mode,
    title: options.title || defaultTitle,
    message: String(message ?? ''),
    confirmText: options.confirmText || confirmText,
    cancelText: options.cancelText || 'No',
    inputValue: options.defaultValue ?? '',
    inputPlaceholder: options.inputPlaceholder || ''
  }

  return new Promise((resolve) => {
    resolver = resolve
  })
}

export function useConfirm() {
  const askConfirm = (message, options = {}) => {
    return openDialog('confirm', message, options)
  }

  const askAlert = (message, options = {}) => {
    return openDialog('alert', message, options)
  }

  const askPrompt = (message, defaultValue = '', options = {}) => {
    return openDialog('prompt', message, {
      ...options,
      defaultValue
    })
  }

  const resolveConfirm = (accepted) => {
    if (confirmState.value.mode === 'prompt') {
      closeWith(accepted ? confirmState.value.inputValue : null)
      return
    }

    if (confirmState.value.mode === 'confirm') {
      closeWith(Boolean(accepted))
      return
    }

    closeWith(undefined)
  }

  const setPromptValue = (value) => {
    confirmState.value = {
      ...confirmState.value,
      inputValue: value
    }
  }

  return {
    confirmState,
    askAlert,
    askConfirm,
    askPrompt,
    resolveConfirm,
    setPromptValue
  }
}
