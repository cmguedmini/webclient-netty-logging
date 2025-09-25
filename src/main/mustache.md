# Add Tags
{{#tags}}
/**
 * Tag: {{name}}
 * {{#description}}Description: {{.}}{{/description}}
 * {{#externalDocs}}{{#description}}External Documentation: {{.}}{{/description}}{{#url}} - {{url}}{{/url}}{{/externalDocs}}
 */
{{/tags}}
