let eventSource = null

function connect() {
  if (eventSource) return
  eventSource = new EventSource('/waveai/api/events')
}

export function useSse() {
  connect()

  function on(eventName, callback) {
    eventSource.addEventListener(eventName, callback)
  }

  function off(eventName, callback) {
    eventSource.removeEventListener(eventName, callback)
  }

  return { on, off }
}
