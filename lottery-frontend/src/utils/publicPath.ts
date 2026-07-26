const absoluteUrlPattern = /^(https?:)?\/\//

export function resolvePublicPath(path: string): string
export function resolvePublicPath(path: string | null | undefined): string | null
export function resolvePublicPath(path: string | null | undefined): string | null {
  if (!path) {
    return null
  }

  if (
    absoluteUrlPattern.test(path)
    || path.startsWith('data:')
    || path.startsWith('blob:')
  ) {
    return path
  }

  if (!path.startsWith('/')) {
    return path
  }

  const base = import.meta.env.BASE_URL || '/'
  const normalizedBase = base.endsWith('/') ? base : `${base}/`

  return `${normalizedBase}${path.slice(1)}`
}
