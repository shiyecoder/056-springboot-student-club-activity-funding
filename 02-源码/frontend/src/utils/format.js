/**
 * 全局时间格式化工具。全系统所有时间展示都必须走这里，避免各处格式不一致。
 */
function pad(n) {
  return n < 10 ? '0' + n : '' + n
}

function toDate(v) {
  if (!v) return null
  if (v instanceof Date) return v
  if (typeof v === 'number') return new Date(v)
  // 兼容 "2026-05-08 14:30:00" 与 ISO 两种写法
  const s = String(v).replace('T', ' ').replace(/-/g, '/')
  const d = new Date(s)
  return isNaN(d.getTime()) ? null : d
}

/** 完整时间：2026-05-08 14:30:00 */
export function fmt(v) {
  const d = toDate(v)
  if (!d) return '-'
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
    ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds())
}

/** 只到日：2026-05-08 */
export function fmtDate(v) {
  const d = toDate(v)
  if (!d) return '-'
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
}

/** 金额：12,345.60 */
export function money(v) {
  if (v === null || v === undefined || v === '') return '0.00'
  const n = Number(v)
  if (isNaN(n)) return '0.00'
  return n.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

export default { fmt, fmtDate, money }
