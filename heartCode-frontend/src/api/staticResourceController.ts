// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 GET /deploy/${param0} */
export async function deployAppIndex(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deployAppIndexParams,
  options?: { [key: string]: any }
) {
  const { key: param0, ...queryParams } = params
  return request<string>(`/deploy/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /deploy/${param0}/ */
export async function deployAppIndexSlash(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deployAppIndexSlashParams,
  options?: { [key: string]: any }
) {
  const { key: param0, ...queryParams } = params
  return request<string>(`/deploy/${param0}/`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /static/vue_project_${param0}/dist */
export async function vueProjectDistIndex(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.vueProjectDistIndexParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<string>(`/static/vue_project_${param0}/dist`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /static/vue_project_${param0}/dist/ */
export async function vueProjectDistIndexSlash(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.vueProjectDistIndexSlashParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<string>(`/static/vue_project_${param0}/dist/`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}
