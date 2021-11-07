import { user } from './store.js'
import { getSanitizingConverter } from 'pagedown';

var converter = getSanitizingConverter();

export const getUser = async () => {
  const response = await fetch('/api/user', {
    method: "get",
    withCredentials: true,
    headers: {
      'Accept': 'application/json'
    }
  });
  if (!response.ok) {
    if (response.status == 404) {
      // User was simpoly not found, therefore not authenticated
      user.set(null);
    } else {
      // Otherwise, something else went wrong
      notifier.danger('Retrieve of user information failed.');
    }
  } else {
    var tmp = await response.json();
    user.set(tmp);
  }
}

export function convert(rawText) {
  let rawMarkup = converter.makeHtml(rawText);
  return rawMarkup.replace(/^(?:<p>)?(.*?)(?:<\/p>)?$/, "$1")
}

export function jsDate(ldt) {
  let date = new Date(ldt[0],ldt[1]-1,ldt[2]);
  date.setHours(ldt[3]);
  date.setMinutes(ldt[4]);
  return date;
}

export function inTheFuture(event) {
  let now = new Date();
  let eventDate = jsDate(event.eventDate);
  let eventEnd = eventDate;
  let hours = Math.floor(event.duration);
  eventEnd.setHours(eventEnd.getHours() + hours);
  if (hours != event.duration) {
    eventEnd.setMinutes(eventEnd.getMinutes() + 30);
  }
  return eventEnd > now;
}

export function toDate(ldt) {
  if (ldt) {
    return ldt[0] + '-' + zeroPad(ldt[1]) + "-" + zeroPad(ldt[2]);
  }
  return null;
}
export function toTime(ldt) {
  if (ldt) {
    return zeroPad(ldt[3]) + ':' + zeroPad(ldt[4]);
  }
  return null;
}
export function toDateTime(ldt) {
  if (ldt) {
    return toDate(ldt) + "  " + toTime(ldt);
  } else {
    return null;
  }
}
function zeroPad(value) {
  if (value < 10)
    return '0' + value;
  else
    return value;
}
