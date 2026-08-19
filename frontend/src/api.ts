import type {
  CreateNodeRequest,
  NarrativeAsset,
  NarrativeDto,
  NarrativeLayer,
  NarrativeNode,
  NarrativeSearchResponse,
  NarrativeTree,
  PositionDto,
  UpdateNodeRequest,
} from './types'

const API_BASE = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')

export class ApiError extends Error {
  status: number

  constructor(message: string, status = 0) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers: {
        ...(init?.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
        ...init?.headers,
      },
    })
  } catch {
    throw new ApiError('Backend не отвечает. Проверьте, что сервис запущен.')
  }

  if (!response.ok) {
    let message = `Ошибка API (${response.status})`
    try {
      const body = await response.json() as { message?: string }
      if (body.message) message = body.message
    } catch {
      // The status code remains useful when the body is not JSON.
    }
    throw new ApiError(message, response.status)
  }

  if (response.status === 204 || response.headers.get('content-length') === '0') {
    return undefined as T
  }

  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}

function narrativePath(projectId: string) {
  return `/projects/${encodeURIComponent(projectId)}/narrative`
}

export const api = {
  getNarrative: (projectId: string) => request<NarrativeDto>(narrativePath(projectId)),
  getTree: (projectId: string) => request<NarrativeTree>(`${narrativePath(projectId)}/tree`),
  getLayer: (projectId: string, layerId: string) =>
    request<NarrativeLayer>(`${narrativePath(projectId)}/layers/${encodeURIComponent(layerId)}`),
  getNode: (projectId: string, nodeId: string) =>
    request<NarrativeNode>(`${narrativePath(projectId)}/nodes/${encodeURIComponent(nodeId)}`),
  createNode: (projectId: string, body: CreateNodeRequest) =>
    request<NarrativeNode>(`${narrativePath(projectId)}/nodes`, { method: 'POST', body: JSON.stringify(body) }),
  updateNode: (projectId: string, nodeId: string, body: UpdateNodeRequest) =>
    request<NarrativeNode>(`${narrativePath(projectId)}/nodes/${encodeURIComponent(nodeId)}`, {
      method: 'PATCH', body: JSON.stringify({
        title: body.title,
        content: body.content,
        linkedNodeIds: body.linkedNodeIds ?? [],
      }),
    }),
  updatePositions: (projectId: string, nodes: Array<{ id: string; position: PositionDto }>) =>
    request<void>(`${narrativePath(projectId)}/node-positions`, {
      method: 'PATCH', body: JSON.stringify({ nodes }),
    }),
  createChildLayer: (projectId: string, nodeId: string) =>
    request<{ id: string; parentNodeId: string }>(
      `${narrativePath(projectId)}/nodes/${encodeURIComponent(nodeId)}/child-layer`, { method: 'POST' },
    ),
  deleteNode: (projectId: string, nodeId: string) =>
    request<void>(`${narrativePath(projectId)}/nodes/${encodeURIComponent(nodeId)}`, { method: 'DELETE' }),
  search: (projectId: string, query: string, page = 0, size = 20) => {
    const params = new URLSearchParams({ q: query, page: String(page), size: String(size) })
    return request<NarrativeSearchResponse>(`${narrativePath(projectId)}/search?${params}`)
  },
  uploadAsset: (projectId: string, file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request<NarrativeAsset>(`${narrativePath(projectId)}/assets`, { method: 'POST', body: formData })
  },
}
