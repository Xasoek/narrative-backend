export type JsonPrimitive = string | number | boolean | null
export type JsonValue = JsonPrimitive | JsonValue[] | { [key: string]: JsonValue }

export type PositionDto = { x: number; y: number }

export type NarrativeDto = {
  id: string
  projectId: string
  rootLayerId: string
}

export type Breadcrumb = {
  layerId: string
  nodeId: string | null
  title: string
}

export type LayerNode = {
  id: string
  title: string
  excerpt: string
  position: PositionDto
  childLayerId: string | null
}

export type NodeReference = { sourceNodeId: string; targetNodeId: string }

export type NarrativeLayer = {
  id: string
  parentNodeId: string | null
  breadcrumbs: Breadcrumb[]
  nodes: LayerNode[]
  references: NodeReference[]
}

export type NarrativeNode = {
  id: string
  layerId: string
  title: string
  content: JsonValue
  position: PositionDto
  childLayerId: string | null
}

export type NarrativeTreeItem = {
  id: string
  title: string
  layerId: string
  parentNodeId: string | null
  childLayerId: string | null
}

export type NarrativeTree = { items: NarrativeTreeItem[] }

export type NarrativeSearchItem = {
  id: string
  layerId: string
  title: string
  content: JsonValue
}

export type NarrativeSearchResponse = {
  query: string
  totalElements: number
  totalPages: number
  page: number
  size: number
  items: NarrativeSearchItem[]
}

export type NarrativeAsset = {
  id: string
  url: string
  mimeType: string
  width: number
  height: number
  size: number
}

export type CreateNodeRequest = {
  layerId: string
  title: string
  content: JsonValue
  position: PositionDto
  linkedNodeIds: string[]
}

export type UpdateNodeRequest = {
  title: string
  content: JsonValue
  linkedNodeIds: string[]
}

export function isJsonRecord(value: JsonValue | undefined): value is { [key: string]: JsonValue } {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}

export function extractText(value: JsonValue | undefined): string {
  const parts: string[] = []

  function walk(node: JsonValue | undefined) {
    if (node === undefined || node === null) return
    if (typeof node === 'string') {
      parts.push(node)
      return
    }
    if (typeof node !== 'object') return
    if (Array.isArray(node)) {
      node.forEach(walk)
      return
    }
    if (typeof node.text === 'string') parts.push(node.text)
    if (node.content) walk(node.content)
  }

  walk(value)
  return parts.join(' ').replace(/\s+/g, ' ').trim()
}

export function withText(value: JsonValue, text: string): JsonValue {
  if (isJsonRecord(value) && typeof value.text === 'string') return { ...value, text }
  const assets = isJsonRecord(value) && Array.isArray(value.assets) ? value.assets : undefined
  return assets ? { text, assets } : { text }
}

export function withAsset(value: JsonValue, asset: NarrativeAsset): JsonValue {
  const base = isJsonRecord(value) ? value : { text: extractText(value) }
  const assets = Array.isArray(base.assets) ? base.assets : []
  return { ...base, assets: [...assets, asset as unknown as JsonValue] }
}

export function extractAssets(value: JsonValue | null): NarrativeAsset[] {
  if (!value || !isJsonRecord(value) || !Array.isArray(value.assets)) return []
  return value.assets
    .filter((item): item is { [key: string]: JsonValue } => isJsonRecord(item))
    .filter((item) => typeof item.url === 'string')
    .map((item) => ({
      id: String(item.id ?? item.url),
      url: String(item.url),
      mimeType: String(item.mimeType ?? 'image/*'),
      width: Number(item.width ?? 0),
      height: Number(item.height ?? 0),
      size: Number(item.size ?? 0),
    }))
}
